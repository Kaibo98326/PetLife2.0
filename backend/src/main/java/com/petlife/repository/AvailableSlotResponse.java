package com.petlife.repository;

public record AvailableSlotResponse(Integer slotId, String slotName, String startTime, String endTime,
        Integer sortOrder) {
}
