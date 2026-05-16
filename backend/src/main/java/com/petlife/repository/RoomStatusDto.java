package com.petlife.repository;

import java.time.LocalDate;
import lombok.Data;

@Data
public class RoomStatusDto {
    private Integer roomId;
    private String roomNo;
    private String roomTypeName;
    private String roomStatus;      // 可預約、維護中

    // 如果有入住訂單才有值
    private Integer stayId;
    private String memberName;
    private String petName;
    private LocalDate stayStartDate;
    private LocalDate stayEndDate;
    private String stayStatus;
    
    private Boolean isOccupied;  // 這天有沒有人住
}