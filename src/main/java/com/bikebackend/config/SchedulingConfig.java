package com.bikebackend.config;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

/** Centralized, overridable working-calendar configuration for job allocation. */
public final class SchedulingConfig {
    private SchedulingConfig() {}
    public static LocalTime workdayStart() { return time("vrm.scheduler.workday-start", LocalTime.of(9, 0)); }
    public static LocalTime lunchStart() { return time("vrm.scheduler.break-start", LocalTime.of(13, 0)); }
    public static LocalTime lunchEnd() { return time("vrm.scheduler.break-end", LocalTime.of(14, 0)); }
    public static LocalTime workdayEnd() { return time("vrm.scheduler.workday-end", LocalTime.of(18, 0)); }
    public static int maxHorizonDays() { return integer("vrm.scheduler.max-horizon-days", 30); }
    public static Set<DayOfWeek> workingDays() {
        String configured = System.getProperty("vrm.scheduler.working-days", "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String value : configured.split(",")) {
            try { days.add(DayOfWeek.valueOf(value.trim().toUpperCase())); }
            catch (IllegalArgumentException ignored) { }
        }
        return days.isEmpty() ? EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY) : days;
    }
    public static boolean durationsAreHours() { return !"MINUTES".equalsIgnoreCase(System.getProperty("vrm.scheduler.duration-unit", "HOURS")); }
    private static LocalTime time(String key, LocalTime fallback) {
        try { return LocalTime.parse(System.getProperty(key, fallback.toString())); }
        catch (RuntimeException e) { return fallback; }
    }
    private static int integer(String key, int fallback) {
        try { return Integer.parseInt(System.getProperty(key, String.valueOf(fallback))); }
        catch (RuntimeException e) { return fallback; }
    }
}
