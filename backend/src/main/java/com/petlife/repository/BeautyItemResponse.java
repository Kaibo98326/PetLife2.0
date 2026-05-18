package com.petlife.repository;

import java.math.BigDecimal;

public record BeautyItemResponse(Integer beautyId, String itemName, String itemDescription, String imageUrl, Integer durationSlots,
        Boolean isActive, BigDecimal itemPrice, String petSize) {
}
