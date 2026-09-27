package com.bikebackend.service;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.config.ScheduleChoice;
import com.bikebackend.config.SchedulingConfig;
import com.bikebackend.config.TimeSlot;
import com.bikebackend.exception.BadRequestException;
import com.bikebackend.exception.ConflictException;
import com.bikebackend.exception.NotFoundException;
import com.bikebackend.model.MechanicAllocation;
import com.bikebackend.model.ScheduleResult;

import java.sql.*;
import java.time.*;
import java.util.*;

/**
 * Finds a complete schedule in memory and persists it atomically. No tentative rows
 * are written while searching; the final transaction locks and rechecks every resource.
 */
public class JobSchedulingService {
    private record ServiceNeed(long jsId, String specialization, long minutes) {}
    private record Busy(LocalDateTime start, LocalDateTime end) {}
    private record Mechanic(long id, String specialization) {}
    private record Bay(long id) {}

    public ScheduleResult allocate(long gid, long jid, LocalDate requestedStart, LocalDate requestedDelivery) throws SQLException {
        if (requestedStart == null || requestedDelivery == null || requestedDelivery.isBefore(requestedStart))
            throw new BadRequestException("requested_start and delivery_date must be valid dates");
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long actualGid = jobGarage(connection, jid);
                if (actualGid != gid) throw new ConflictException("Job does not belong to this garage");
                List<ServiceNeed> needs = loadNeeds(connection, gid, jid);
                if (needs.isEmpty()) throw new BadRequestException("Job has no services to schedule");
                for (int extension = 0; extension <= SchedulingConfig.maxHorizonDays(); extension++) {
                    LocalDate windowEnd = requestedDelivery.plusDays(extension);
                    Candidate candidate = findCandidate(connection, gid, jid, needs, requestedStart, windowEnd);
                    if (candidate != null) {
                        persist(connection, jid, candidate);
                        connection.commit();
                        return candidate.result;
                    }
                }
                connection.rollback();
                throw new ConflictException("No combination of mechanics and bays is available within the configured scheduling horizon");
            } catch (Exception e) {
                try { connection.rollback(); } catch (SQLException ignored) { }
                if (e instanceof SQLException sql) throw sql;
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new SQLException("Scheduling failed", e);
            } finally { connection.setAutoCommit(true); }
        }
    }

    private Candidate findCandidate(Connection c, long gid, long jid, List<ServiceNeed> needs, LocalDate from, LocalDate to) throws SQLException {
        LocalDateTime windowStart = from.atTime(SchedulingConfig.workdayStart());
        LocalDateTime windowEnd = to.atTime(SchedulingConfig.workdayEnd());
        Map<Long, List<Busy>> busy = loadBusy(c, windowStart, windowEnd);
        Map<String, List<Mechanic>> mechanics = loadMechanics(c, gid, needs);
        for (ServiceNeed need : needs) if (mechanics.getOrDefault(need.specialization, List.of()).isEmpty()) return null;
        List<ServiceNeed> ordered = new ArrayList<>(needs);
        ordered.sort(Comparator.comparingInt(n -> mechanics.getOrDefault(n.specialization, List.of()).size()));
        List<ScheduleChoice> selected = new ArrayList<>();
        if (!backtrack(ordered, 0, mechanics, busy, windowStart, windowEnd, selected)) return null;
        LocalDateTime jobStart = selected.stream().map(ScheduleChoice::start).min(LocalDateTime::compareTo).orElseThrow();
        LocalDateTime jobEnd = selected.stream().map(ScheduleChoice::end).max(LocalDateTime::compareTo).orElseThrow();
        Long bayId = findAvailableBay(c, gid, jobStart.toLocalDate(), jobEnd.toLocalDate());
        if (bayId == null) return null;
        List<MechanicAllocation> allocations = selected.stream().map(x -> new MechanicAllocation(x.jsId(), x.mechanicId(), x.start(), x.end())).toList();
        return new Candidate(new ScheduleResult(jid, jobStart, jobEnd, bayId, allocations), selected);
    }

    private boolean backtrack(List<ServiceNeed> needs, int index, Map<String,List<Mechanic>> mechanics, Map<Long,List<Busy>> busy,
                              LocalDateTime windowStart, LocalDateTime windowEnd, List<ScheduleChoice> selected) {
        if (index == needs.size()) return true;
        ServiceNeed need = needs.get(index);
        for (Mechanic mechanic : mechanics.getOrDefault(need.specialization, List.of())) {
            List<Busy> occupied = new ArrayList<>(busy.getOrDefault(mechanic.id, List.of()));
            for (ScheduleChoice choice : selected) if (choice.mechanicId() == mechanic.id) occupied.add(new Busy(choice.start(), choice.end()));
            for (TimeSlot slot : availableSlots(windowStart, windowEnd, occupied, need.minutes)) {
                selected.add(new ScheduleChoice(need.jsId, mechanic.id, slot.start(), slot.end()));
                if (backtrack(needs, index + 1, mechanics, busy, windowStart, windowEnd, selected)) return true;
                selected.remove(selected.size() - 1);
            }
        }
        return false;
    }

    private List<TimeSlot> availableSlots(LocalDateTime from, LocalDateTime to, List<Busy> busy, long requiredMinutes) {
        List<TimeSlot> result = new ArrayList<>();
        for (LocalDate day = from.toLocalDate(); !day.isAfter(to.toLocalDate()); day = day.plusDays(1)) {
            if (!SchedulingConfig.workingDays().contains(day.getDayOfWeek())) continue;
            LocalDateTime dayStart = day.atTime(SchedulingConfig.workdayStart()), dayEnd = day.atTime(SchedulingConfig.workdayEnd());
            LocalDateTime lower = dayStart.isAfter(from) ? dayStart : from, upper = dayEnd.isBefore(to) ? dayEnd : to;
            for (TimeSlot work : workingSegments(lower, upper)) {
                List<Busy> conflicts = busy.stream().filter(b -> b.start().isBefore(work.end()) && b.end().isAfter(work.start())).sorted(Comparator.comparing(Busy::start)).toList();
                LocalDateTime cursor = work.start();
                for (Busy b : conflicts) {
                    if (cursor.isBefore(b.start())) addIfLongEnough(result, cursor, b.start(), requiredMinutes);
                    if (b.end().isAfter(cursor)) cursor = b.end().isBefore(work.end()) ? b.end() : work.end();
                }
                if (cursor.isBefore(work.end())) addIfLongEnough(result, cursor, work.end(), requiredMinutes);
            }
        }
        return result;
    }
    private List<TimeSlot> workingSegments(LocalDateTime start, LocalDateTime end) {
        List<TimeSlot> result = new ArrayList<>();
        LocalDateTime lunchStart = start.toLocalDate().atTime(SchedulingConfig.lunchStart()), lunchEnd = start.toLocalDate().atTime(SchedulingConfig.lunchEnd());
        if (start.isBefore(lunchStart)) result.add(new TimeSlot(start, end.isBefore(lunchStart) ? end : lunchStart));
        if (end.isAfter(lunchEnd)) result.add(new TimeSlot(start.isAfter(lunchEnd) ? start : lunchEnd, end));
        return result;
    }
    private void addIfLongEnough(List<TimeSlot> out, LocalDateTime start, LocalDateTime end, long minutes) {
        if (Duration.between(start, end).toMinutes() >= minutes) out.add(new TimeSlot(start, start.plusMinutes(minutes)));
    }

    private Map<Long,List<Busy>> loadBusy(Connection c, LocalDateTime from, LocalDateTime to) throws SQLException {
        Map<Long,List<Busy>> result = new HashMap<>();
        String sql = "SELECT mechanic_id,start_datetime,end_datetime FROM mechanic_allocations WHERE status <> 'CANCELLED' AND start_datetime < ? AND end_datetime > ? ORDER BY start_datetime";
        try (PreparedStatement ps=c.prepareStatement(sql)) { ps.setTimestamp(1, Timestamp.valueOf(to)); ps.setTimestamp(2, Timestamp.valueOf(from)); try (ResultSet rs=ps.executeQuery()) { while(rs.next()) result.computeIfAbsent(rs.getLong(1), k->new ArrayList<>()).add(new Busy(rs.getTimestamp(2).toLocalDateTime(), rs.getTimestamp(3).toLocalDateTime())); } }
        return result;
    }
    private Map<String,List<Mechanic>> loadMechanics(Connection c, long gid, List<ServiceNeed> needs) throws SQLException {
        Set<String> specs = needs.stream().map(ServiceNeed::specialization).collect(java.util.stream.Collectors.toSet());
        Map<String,List<Mechanic>> result = new HashMap<>();
        String sql = "SELECT mechanic_id,specialization FROM mechanics WHERE gid=? AND specialization=?";
        for (String spec : specs) try (PreparedStatement ps=c.prepareStatement(sql)) { ps.setLong(1,gid); ps.setString(2,spec); try(ResultSet rs=ps.executeQuery()) { while(rs.next()) result.computeIfAbsent(spec,k->new ArrayList<>()).add(new Mechanic(rs.getLong(1),rs.getString(2))); } }
        return result;
    }
    private List<ServiceNeed> loadNeeds(Connection c, long gid, long jid) throws SQLException {
        String sql = "SELECT js.js_id, COALESCE(NULLIF(s.specialization,''), s.service_type), gs.duration FROM job_services js JOIN services s ON s.sid=js.sid JOIN garage_services gs ON gs.sid=js.sid AND gs.gid=? WHERE js.jid=? AND js.status <> 'REJECTED' ORDER BY js.js_id";
        List<ServiceNeed> out=new ArrayList<>();
        try(PreparedStatement ps=c.prepareStatement(sql)){ps.setLong(1,gid);ps.setLong(2,jid);try(ResultSet rs=ps.executeQuery()){while(rs.next()){Integer duration=(Integer)rs.getObject(3); if(duration==null||duration<=0) throw new BadRequestException("Every job service must have a positive duration"); out.add(new ServiceNeed(rs.getLong(1),rs.getString(2),SchedulingConfig.durationsAreHours()?duration*60L:duration));}}}
        return out;
    }
    private Long findAvailableBay(Connection c,long gid,LocalDate start,LocalDate end)throws SQLException{
        String sql="SELECT gb.bay_id FROM garage_bays gb WHERE gb.gid=? AND NOT EXISTS (SELECT 1 FROM bay_allocations ba WHERE ba.bay_id=gb.bay_id AND ba.status='ACTIVE' AND ba.start_date < ? AND ba.end_date > ?) ORDER BY gb.bay_id FOR UPDATE";
        try(PreparedStatement ps=c.prepareStatement(sql)){ps.setLong(1,gid);ps.setObject(2,end);ps.setObject(3,start);try(ResultSet rs=ps.executeQuery()){return rs.next()?rs.getLong(1):null;}}
    }
    private long jobGarage(Connection c,long jid)throws SQLException{try(PreparedStatement ps=c.prepareStatement("SELECT sr.gid FROM jobs j JOIN service_requests sr ON sr.sr_id=j.sr_id WHERE j.jid=?")){ps.setLong(1,jid);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new NotFoundException("Job not found");return rs.getLong(1);}}}
    private void persist(Connection c,long jid,Candidate candidate)throws SQLException{
        String conflictSql = "SELECT 1 FROM mechanic_allocations WHERE mechanic_id=? AND status <> 'CANCELLED' AND start_datetime < ? AND end_datetime > ? FOR UPDATE";
        try (PreparedStatement ps = c.prepareStatement(conflictSql)) {
            for (ScheduleChoice x : candidate.choices) {
                ps.setLong(1, x.mechanicId());
                ps.setTimestamp(2, Timestamp.valueOf(x.end()));
                ps.setTimestamp(3, Timestamp.valueOf(x.start()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) throw new ConflictException("A mechanic allocation changed while the schedule was being committed");
                }
            }
        }
        String a="INSERT INTO mechanic_allocations(mechanic_id,jid,js_id,start_datetime,end_datetime,status) VALUES(?,?,?,?,?,'ALLOCATED')";
        try(PreparedStatement ps=c.prepareStatement(a)){for(ScheduleChoice x:candidate.choices){ps.setLong(1,x.mechanicId());ps.setLong(2,jid);ps.setLong(3,x.jsId());ps.setTimestamp(4,Timestamp.valueOf(x.start()));ps.setTimestamp(5,Timestamp.valueOf(x.end()));ps.addBatch();}ps.executeBatch();}
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO bay_allocations(bay_id,jid,start_date,end_date,status) VALUES(?,?,?,?,'ACTIVE')")){ps.setLong(1,candidate.result.getBayId());ps.setLong(2,jid);ps.setObject(3,candidate.result.getJobStart().toLocalDate());ps.setObject(4,candidate.result.getJobEnd().toLocalDate());ps.executeUpdate();}
        try(PreparedStatement ps=c.prepareStatement("UPDATE jobs SET service_start_date=?,service_end_date=?,expected_delivery_date=? WHERE jid=?")){ps.setTimestamp(1,Timestamp.valueOf(candidate.result.getJobStart()));ps.setTimestamp(2,Timestamp.valueOf(candidate.result.getJobEnd()));ps.setObject(3,candidate.result.getDeliveryDate());ps.setLong(4,jid);ps.executeUpdate();}
    }
    private record Candidate(ScheduleResult result,List<ScheduleChoice> choices) {}
}
