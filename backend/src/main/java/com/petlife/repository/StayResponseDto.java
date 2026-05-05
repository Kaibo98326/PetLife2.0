package com.petlife.repository;

import java.time.LocalDate;

import lombok.Data;

@Data
public class StayResponseDto {

	// Stay 基本資料
    private Integer stayId;
    private LocalDate stayStartDate;
    private LocalDate stayEndDate;
    private Integer stayDay;
    private Integer petCount;
    private Double sumPrice;
    private String stayStatus;

    // 來自 Pet
    private String petName;

    // 來自 StayRoomType
    private String roomTypeName;
    
    // 來自 StayRoom
    private String roomNo;

    // 來自 StayPayment
    private String paymentMethod;
    private String paymentStatus;
}
