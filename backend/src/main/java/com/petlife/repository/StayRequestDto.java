package com.petlife.repository;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class StayRequestDto {

	private Integer  petId;
	
	private Integer  stayRoomTypeId;
	
	private LocalDate stayStartDate;
	
	private LocalDate stayEndDate;
	
	private Integer petCount;
	
	private String customerNote; // 客人備註
	
	private String paymentMethod;

    private List<Integer> extraPetIds; // 第2隻以後的寵物 ID
}
