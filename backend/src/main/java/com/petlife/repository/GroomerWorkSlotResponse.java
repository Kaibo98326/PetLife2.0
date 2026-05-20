package com.petlife.repository;

import java.time.LocalDate;

public record GroomerWorkSlotResponse(Integer workSlotId, Integer groomerId, LocalDate workDate, Integer slotId,
        Integer appointmentId, String workSlotStatus, String note) {
}
