package com.petlife.controller;

import com.petlife.repository.*;
import com.petlife.service.IStayService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/stay")
@RequiredArgsConstructor
public class AdminStayController {
	
	private final IStayService stayService;
	
	// ========== 訂單管理 ==========
	
	/**
	 * 1. 查詢訂單列表（含搜尋、分頁）
	 * 支援搜尋：訂單編號、會員名稱、電話末三碼、訂單狀態、日期範圍
	 */
	@GetMapping
	public ResponseEntity<Page<AdminStayResponseDto>> getAllStays(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String stayStatus,
			@RequestParam(required = false) String memberName,
			@RequestParam(required = false) String memberPhone,
			@RequestParam(required = false) String stayId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
			) {
		
		AdminStayQueryDto query = new AdminStayQueryDto();
		query.setPage(page);
		query.setSize(size);
		query.setStayStatus(stayStatus);
		query.setMemberName(memberName);
		query.setMemberPhone(memberPhone);
		query.setStayId(stayId);
		query.setStartDate(startDate);
		query.setEndDate(endDate);
		
		return ResponseEntity.ok(stayService.getAllStaysForAdmin(query));
	}
	
	/**
	 * 2. 查詢單筆訂單詳情
	 */
	@GetMapping("/{stayId}")
	public ResponseEntity<AdminStayResponseDto> getStayDetail(@PathVariable Integer stayId) {
		return ResponseEntity.ok(stayService.getStayByIdForAdmin(stayId));
	}
	
	/**
	 * 3. 修改訂單狀態
	 * 狀態：PENDING_PAYMENT（待支付）-> CONFIRMED（已確認）-> CHECKED_IN（已入住）-> CHECKED_OUT（已退房）
	 * 或 -> CANCELLED（已取消）
	 */
	@PatchMapping("/{stayId}/status")
	public ResponseEntity<AdminStayResponseDto> updateStayStatus(
			@PathVariable Integer stayId,
			@RequestParam String newStatus) {
		return ResponseEntity.ok(stayService.updateStayStatus(stayId, newStatus));
	}
	
	/**
	 * 4. 取消訂單（會同時更新房間狀態為可預約）
	 */
	@PatchMapping("/{stayId}/cancel")
	public ResponseEntity<AdminStayResponseDto> cancelStayByAdmin(
			@PathVariable Integer stayId) {
		return ResponseEntity.ok(stayService.cancelStayByAdmin(stayId));
	}
	
	// ========== 房間管理 ==========
	
	/**
	 * 5. 查詢所有房間
	 */
	@GetMapping("/room")
	public ResponseEntity<List<StayRoomDto>> getAllRooms() {
		return ResponseEntity.ok(stayService.getAllRooms());
	}
	
	/**
	 * 6. 修改房間狀態（停權/啟用）
	 * 狀態：可預約、維護中
	 */
	@PatchMapping("/room/{roomId}/status")
	public ResponseEntity<StayRoomDto> updateRoomStatus(
			@PathVariable Integer roomId,
			@RequestParam String status) {
		return ResponseEntity.ok(stayService.updateRoomStatus(roomId, status));
	}
	
	// ========== 房型管理 ==========
	
	/**
	 * 7. 修改房型價格
	 */
	@PatchMapping("/roomtype/{roomTypeId}/price")
	public ResponseEntity<RoomTypeDto> updateRoomTypePrice(
			@PathVariable Integer roomTypeId,
			@RequestParam Double newPrice) {
		return ResponseEntity.ok(stayService.updateRoomTypePrice(roomTypeId, newPrice));
	}
	
	// ========== 日期查詢 ==========
	
	/**
	 * 8. 查看日期範圍內所有房間狀態
	 */
	@GetMapping("/calendar/rooms")
	public ResponseEntity<List<RoomCalendarDto>> getRoomCalendar(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		return ResponseEntity.ok(stayService.getRoomCalendar(startDate, endDate));
	}
	
}