package com.petlife.service;

import java.time.LocalDate;
import java.util.List;

import com.petlife.repository.CalendarDayDto;
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
	
	// 從前端得到 ? 年 ? 月 回傳 此月每一號可用空房
	List<CalendarDayDto> getCalendar(Integer roomTypeId,int year,int month);
	
	// 查詢所有房型
	List<RoomTypeDto> getAllRoomTypes();
	
	// 取得房型ID
	RoomTypeDto getRoomTypeById(Integer roomTypeId);
	
}
