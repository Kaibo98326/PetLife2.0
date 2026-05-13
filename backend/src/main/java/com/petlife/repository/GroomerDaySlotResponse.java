package com.petlife.repository;

import java.time.LocalDate;
import java.util.List;

public record GroomerDaySlotResponse(Integer groomerId, LocalDate workDate, String scheduleStatus, String scheduleNote,
        List<GroomerDaySlotLineResponse> slots) {
}
