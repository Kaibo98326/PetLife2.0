package com.petlife.service;

import java.time.LocalDate;
import java.util.List;


import com.petlife.repository.CalendarDayDto;
import com.petlife.repository.RoomStatusDto;
import com.petlife.repository.RoomTypeDto;
import com.petlife.repository.StayPaymentResponseDto;
import com.petlife.repository.StayRequestDto;
import com.petlife.repository.StayResponseDto;
import com.petlife.repository.StayRoomDto;

public interface IStayService {

	// 計價邏輯
	Double calculatePrice(Integer roomTypeId,LocalDate startDate,LocalDate endDate);
	
	// 空房查詢
	RoomTypeDto checkAvailability(Integer roomTypeId,LocalDate startDate,LocalDate endDate);
	
	// 建立預約
	StayResponseDto createStay(StayRequestDto  request);
	
	// 取消預約
	void cancelStay(Integer stayId);
	 
	// 查單筆訂單
	StayResponseDto getStayById(Integer stayId);
	
	// 取得會員訂單
	List<StayResponseDto> getMyStays(Integer memberId);
	
	// 從前端得到 ? 年 ? 月 回傳 此月每一號可用空房
	List<CalendarDayDto> getCalendar(Integer roomTypeId,int year,int month);
	
	// 查詢所有房型
	List<RoomTypeDto> getAllRoomTypes();
	
	// 取得房型ID
	RoomTypeDto getRoomTypeById(Integer roomTypeId);
	
	// 建立訂單 與 line pay
	StayPaymentResponseDto createStayWithPayment(StayRequestDto request);
	
	// 確認預約
	String confirmPayment(String merchantTradeNo, String transactionId);
	
	// 付款紀錄
	Integer getStayIdByMerchantTradeNo(String merchantTradeNo);
	
	// ----------------------------後臺方法----------------------
	
	// 全部訂單
	List<StayResponseDto> getAllStays();
		
	// 修改訂單狀態
	void updateStayStatus(Integer stayId, String status);
	
	// 查所有房間
	List<StayRoomDto> getAllRooms();

	// 修改房間狀態
	void updateRoomStatus(Integer roomId, String status);
	
	// 修改房型資料
	RoomTypeDto updateRoomType(Integer roomTypeId, Double newPrice, 
	        String roomName, Integer capacity, String roomDescription);


	// 訂單的三個搜尋
	// 會員名字 
	List<StayResponseDto> searchByMemberName(String name);
	// 訂單編號
	List<StayResponseDto> searchByStayId(Integer stayId);
	// 手機末三碼
	List<StayResponseDto> searchByPhone(String phone);
	
	// 修改房間狀態邏輯
	List<RoomStatusDto> getRoomStatusByDate(LocalDate date);
}

	
	

