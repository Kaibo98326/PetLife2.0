package com.petlife.repository;

import java.math.BigDecimal;

public record AppointmentDetailLineResponse(Integer detailId, Integer lineNo, Integer beautyId, String itemNameSnapshot,
        BigDecimal itemPriceSnapshot, Integer durationSlotsSnapshot, String lineNote) {
}
