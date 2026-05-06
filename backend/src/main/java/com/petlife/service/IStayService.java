package com.petlife.service;

import java.time.LocalDate;
import java.util.List;

import com.petlife.repository.RoomTypeDto;
import com.petlife.repository.StayRequestDto;
import com.petlife.repository.StayResponseDto;

public interface IStayService {

	// 計價邏輯
	Double calculatePrice(Integer roomTypeId,LocalDate startDate,LocalDate endDate);
	
	// 空房查詢
	RoomTypeDto checkAvailability(Integer roomTypeId,LocalDate startDate,LocalDate endDate);
	
	// 建立預約
	StayResponseDto createStay(StayRequestDto  request);
	
	// 取消預約
	void cancelStay(Integer stayId);
	 
	// 取得會員訂單
	List<StayResponseDto> getMyStays(Integer memberId);
}
