package com.petlife.repository;

import java.util.List;

public record BeautyItemManageResponse(Integer beautyId, String itemName, String itemDescription,
        Integer durationSlots, Boolean isActive, List<BeautyPriceResponse> prices) {
}
