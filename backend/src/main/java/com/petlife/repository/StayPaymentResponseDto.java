package com.petlife.repository;

import lombok.Data;

@Data
public class StayPaymentResponseDto {
    private Integer stayId;
    private String paymentUrl;
}