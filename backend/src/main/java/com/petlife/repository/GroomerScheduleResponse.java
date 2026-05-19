package com.petlife.repository;

import java.time.LocalDate;

public record GroomerScheduleResponse(Integer scheduleId, Integer groomerId, LocalDate workDate,
        String scheduleStatus, String note) {
}
