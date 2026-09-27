package com.bikebackend.config;

import java.time.LocalDateTime;

public record TimeSlot(LocalDateTime start, LocalDateTime end) {}
