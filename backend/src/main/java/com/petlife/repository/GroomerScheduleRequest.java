package com.petlife.repository;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GroomerScheduleRequest(@NotNull Integer groomerId, @NotNull LocalDate workDate, String scheduleStatus,
        String note) {
}
