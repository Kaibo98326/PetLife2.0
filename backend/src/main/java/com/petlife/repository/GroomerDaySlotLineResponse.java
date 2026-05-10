package com.petlife.repository;

public record GroomerDaySlotLineResponse(Integer slotId, String slotName, String startTime, String endTime,
        Integer sortOrder, String slotStatus, Integer appointmentId, Integer workSlotId, String note) {
}
