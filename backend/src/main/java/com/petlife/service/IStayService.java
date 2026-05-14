package com.petlife.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;

import com.petlife.repository.AdminStayQueryDto;
import com.petlife.repository.AdminStayResponseDto;
import com.petlife.repository.CalendarDayDto;
import com.petlife.repository.RoomCalendarDto;
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
	
	// ========== 新增方法（後台訂單管理）==========
	
		/**
		 * 後台查詢訂單列表（支援分頁、搜尋）
		 * 搜尋條件：訂單編號、會員名稱、電話末三碼、訂單狀態、日期範圍
		 */
		Page<AdminStayResponseDto> getAllStaysForAdmin(AdminStayQueryDto query);
		
		/**
		 * 後台查詢單筆訂單詳情
		 */
		AdminStayResponseDto getStayByIdForAdmin(Integer stayId);
		
		/**
		 * 修改訂單狀態
		 * 狀態轉移：PENDING_PAYMENT -> CONFIRMED -> CHECKED_IN -> CHECKED_OUT
		 *         或 -> CANCELLED
		 */
		AdminStayResponseDto updateStayStatus(Integer stayId, String newStatus);
		
		/**
		 * 後台取消訂單（會同時更新房間狀態為可預約）
		 */
		AdminStayResponseDto cancelStayByAdmin(Integer stayId);
		
		// ========== 新增方法（房間管理）==========
		
		/**
		 * 查詢所有房間
		 */
		List<StayRoomDto> getAllRooms();
		
		/**
		 * 修改房間狀態（停權/啟用）
		 * 狀態：可預約、維護中
		 */
		StayRoomDto updateRoomStatus(Integer roomId, String status);
		
		// ========== 新增方法（房型管理）==========
		
		/**
		 * 修改房型價格
		 */
		RoomTypeDto updateRoomTypePrice(Integer roomTypeId, Double newPrice);
		
		// ========== 新增方法（日期查詢）==========
		
		
		List<RoomCalendarDto> getRoomCalendar(LocalDate startDate, LocalDate endDate);
		
	}
	

