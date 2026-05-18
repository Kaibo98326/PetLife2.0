package com.petlife.repository;

import lombok.Data;

@Data
public class RoomTypeDto {

	
	private Integer roomTypeId;
	
	private String roomName;
	
	private Double roomPrice;
	
	private String roomDescription;
	
	private Integer capacity;
	
	// 後端計算可用數量
	 private Integer availableCount;
}
