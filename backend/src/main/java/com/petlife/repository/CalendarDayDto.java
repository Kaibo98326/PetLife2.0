package com.petlife.repository;

import java.time.LocalDate;

import lombok.Data;

@Data
public class CalendarDayDto {

    private Integer stayRoomTypeId;   // 哪個房型

    private LocalDate date;           // 哪一天

    private Integer availableCount;   // 這房型這天剩幾間

    private Boolean isAvailable;      // 有沒有空房
	
}
