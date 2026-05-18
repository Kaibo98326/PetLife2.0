package com.petlife.repository;

import java.math.BigDecimal;

public record BeautyPriceResponse(Integer priceId, Integer beautyId, String petSize, BigDecimal itemPrice,
        Boolean isActive) {
}
