package com.petlife.repository;

public record GroomerServiceResponse(Integer groomerBeautyItemId, Integer groomerId, Integer beautyId,
        Boolean isActive, String note) {
}
