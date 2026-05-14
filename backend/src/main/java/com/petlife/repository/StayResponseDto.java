package com.petlife.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private String stayRemark;
    
    private String memberName;
    private String memberPhone;
    private String memberEmail;
    private LocalDateTime createdAt;   // 訂單成立時間
    private LocalDateTime paidAt;      // 付款時間
    
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
