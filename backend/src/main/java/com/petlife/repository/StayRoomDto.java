package com.petlife.repository;

import lombok.Data;

@Data
public class StayRoomDto {
	private Integer roomId;
	private String roomNo;
	private Integer roomTypeId;
	private String roomTypeName;
	private String roomStatus; // "可預約" 或 "維護中"
}