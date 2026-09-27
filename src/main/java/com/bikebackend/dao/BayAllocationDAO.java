package com.bikebackend.dao;

import com.bikebackend.config.DatabaseConnection;
import com.bikebackend.model.BayAllocation;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BayAllocationDAO {

    public long createAllocation(BayAllocation allocation) throws SQLException {

        String sql = """
                INSERT INTO bay_allocations (bay_id, jid, start_date, end_date, status)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            st.setLong(1, allocation.getBayId());
            st.setLong(2, allocation.getJid());
            st.setObject(3, allocation.getStartDate());
            st.setObject(4, allocation.getEndDate());
            st.setString(5, allocation.getStatus() != null ? allocation.getStatus() : "ACTIVE");
            st.executeUpdate();

            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }

        throw new SQLException("Allocation ID was not generated");
    }

    public List<BayAllocation> getByGarage(long gid) throws SQLException {

        String sql = """
                SELECT ba.allocation_id, ba.bay_id, ba.jid, ba.start_date, ba.end_date, ba.status
                FROM bay_allocations ba
                JOIN garage_bays gb ON ba.bay_id = gb.bay_id
                WHERE gb.gid = ?
                ORDER BY ba.start_date DESC
                """;

        List<BayAllocation> list = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, gid);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) list.add(mapAllocation(rs));
            }
        }

        return list;
    }

    public BayAllocation getById(long allocationId) throws SQLException {

        String sql = """
                SELECT allocation_id, bay_id, jid, start_date, end_date, status
                FROM bay_allocations WHERE allocation_id = ?
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, allocationId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return mapAllocation(rs);
            }
        }

        return null;
    }

    /**
     * Checks if any active allocation already occupies a bay on the requested date range.
     * Used BEFORE creating a new allocation to detect conflicts.
     *
     * Two date ranges [s1,e1] and [s2,e2] overlap when: s1 <= e2 AND s2 <= e1
     */
    public boolean hasConflict(long bayId, LocalDate startDate, LocalDate endDate)
            throws SQLException {

        String sql = """
                SELECT 1 FROM bay_allocations
                WHERE bay_id = ?
                  AND status = 'ACTIVE'
                  AND start_date <= ?
                  AND end_date   >= ?
                LIMIT 1
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setLong(1, bayId);
            st.setObject(2, endDate);
            st.setObject(3, startDate);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Bay availability for a garage on a specific date.
     * Returns a list of bays with their allocation availability.
     */
    public List<Map<String, Object>> getAvailability(long gid, LocalDate date)
            throws SQLException {

        String sql = """
                SELECT gb.bay_id, gb.bay_name,
                       CASE WHEN ba.bay_id IS NOT NULL THEN 'OCCUPIED' ELSE 'AVAILABLE' END AS availability
                FROM garage_bays gb
                LEFT JOIN bay_allocations ba
                    ON gb.bay_id = ba.bay_id
                    AND ba.status = 'ACTIVE'
                    AND ba.start_date <= ?
                    AND ba.end_date   >= ?
                WHERE gb.gid = ?
                ORDER BY gb.bay_id ASC
                """;

        List<Map<String, Object>> list = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setObject(1, date);
            st.setObject(2, date);
            st.setLong(3, gid);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("bay_id",       rs.getLong("bay_id"));
                    row.put("bay_name",     rs.getString("bay_name"));
                    row.put("availability", rs.getString("availability"));
                    list.add(row);
                }
            }
        }

        return list;
    }

    public boolean updateStatus(long allocationId, String status) throws SQLException {

        String sql = "UPDATE bay_allocations SET status = ?, version = version + 1 WHERE allocation_id = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement st = connection.prepareStatement(sql)
        ) {
            st.setString(1, status);
            st.setLong(2, allocationId);
            return st.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(long allocationId, String status, int expectedVersion) throws SQLException {
        String sql = "UPDATE bay_allocations SET status = ?, version = version + 1 WHERE allocation_id = ? AND version = ?";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement st = connection.prepareStatement(sql)) {
            st.setString(1, status); st.setLong(2, allocationId); st.setInt(3, expectedVersion);
            return st.executeUpdate() > 0;
        }
    }

    private BayAllocation mapAllocation(ResultSet rs) throws SQLException {
        BayAllocation ba = new BayAllocation();
        ba.setAllocationId(rs.getLong("allocation_id"));
        ba.setBayId(rs.getLong("bay_id"));
        ba.setJid(rs.getLong("jid"));
        Date sd = rs.getDate("start_date");
        if (sd != null) ba.setStartDate(sd.toLocalDate());
        Date ed = rs.getDate("end_date");
        if (ed != null) ba.setEndDate(ed.toLocalDate());
        ba.setStatus(rs.getString("status"));
        return ba;
    }
}
