package com.bikebackend.config;

import java.time.LocalDateTime;

public record ScheduleChoice(
        long jsId, long mechanicId, LocalDateTime start, LocalDateTime end) {}
