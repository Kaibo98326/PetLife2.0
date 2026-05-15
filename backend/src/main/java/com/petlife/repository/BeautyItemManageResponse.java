package com.petlife.repository;

import java.util.List;

public record BeautyItemManageResponse(Integer beautyId, String itemName, String itemDescription, String imageUrl,
        Integer durationSlots, Boolean isActive, List<BeautyPriceResponse> prices) {
}
