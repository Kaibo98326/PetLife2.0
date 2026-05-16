package com.petlife.repository;

import java.time.LocalDate;

public record GroomerMonthlyScheduleDayResponse(LocalDate workDate, String scheduleStatus,
        Integer bookedSlotCount, Integer scheduleClosedSlotCount, Integer availableSlotCount) {
}
