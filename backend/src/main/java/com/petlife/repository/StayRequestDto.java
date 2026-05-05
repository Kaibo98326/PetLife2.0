package com.petlife.repository;

import java.time.LocalDate;

import lombok.Data;

@Data
public class StayRequestDto {

	private Integer  petId;
	
	private Integer  stayRoomTypeId;
	
	private LocalDate stayStartDate;
	
	private LocalDate stayEndDate;
	
	private Integer petCount;
}
