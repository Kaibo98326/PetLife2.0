package com.petlife.repository;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record BlockWorkSlotRequest(@NotNull Integer groomerId, @NotNull LocalDate workDate,
        @NotNull List<Integer> slotIds, String note) {
}
