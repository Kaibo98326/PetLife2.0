package com.petlife.repository;

import java.time.LocalDate;
import lombok.Data;

@Data
public class RoomCalendarDto {
	private Integer roomId;
	private String roomNo;
	private Integer roomTypeId;
	private String roomTypeName;
	private LocalDate date;
	private String status; // "可用" "已預約" "維護中"
	private String memberName; // 如果已預約，顯示會員名
	private String stayId; // 如果已預約，顯示訂單 ID
}