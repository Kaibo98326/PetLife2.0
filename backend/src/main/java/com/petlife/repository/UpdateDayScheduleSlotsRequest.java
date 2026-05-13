package com.petlife.repository;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record UpdateDayScheduleSlotsRequest(@NotNull Integer groomerId, @NotNull LocalDate workDate,
        String scheduleStatus, String note, List<Integer> bookableSlotIds) {
}
