package com.petlife.controller;

import com.petlife.repository.StayRequestDto;
import com.petlife.repository.StayResponseDto;
import com.petlife.repository.StayRoomDto;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petlife.model.StayPayment;
import com.petlife.repository.CalendarDayDto;
import com.petlife.repository.RoomStatusDto;
import com.petlife.repository.RoomTypeDto;
import com.petlife.repository.StayPaymentResponseDto;
import com.petlife.service.IStayService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.cfg.DateTimeFeature;

@RestController
@RequestMapping("/api/stay")
@RequiredArgsConstructor
public class StayController {
	
	
	private final IStayService stayService;


	// 查詢可用房型
	@GetMapping("/available")
	public ResponseEntity<RoomTypeDto> checkAvailability(
			@RequestParam Integer roomTypeId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, 
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate 
			){
		RoomTypeDto result = stayService.checkAvailability(roomTypeId,startDate,endDate);
		 return ResponseEntity.ok(result);
	}
	
//	// 建立預約
//	@PostMapping
//	public ResponseEntity<StayResponseDto> createStay(@RequestBody StayRequestDto request){
//		StayResponseDto result = stayService.createStay(request);
//		return ResponseEntity.ok(result);
//	}
	
	
	
	// 取消預約
	@PatchMapping("/{stayId}/cancel")
	public ResponseEntity<Void> cancelStay(@PathVariable Integer stayId){
		stayService.cancelStay(stayId);
		return ResponseEntity.ok().build();
	}
	
	// 我的訂單
	@GetMapping("/member/{memberId}")
	public ResponseEntity<List<StayResponseDto>> getMyStays(@PathVariable Integer memberId){
		List<StayResponseDto> result = stayService.getMyStays(memberId);
		return ResponseEntity.ok(result);
		
	}
	
	// 行事曆查詢
	@GetMapping("/calendar")
	public ResponseEntity<List<CalendarDayDto>> getCalendar(@RequestParam Integer roomTypeId,
			@RequestParam int year,
			@RequestParam int month
			){
	List<CalendarDayDto> result = stayService.getCalendar(roomTypeId, year, month);
	return ResponseEntity.ok(result);
	}
	
	// 所有房型列表
	@GetMapping("roomtype")
	public ResponseEntity<List<RoomTypeDto>> getAllRoomTypes(){
		List<RoomTypeDto> result = stayService.getAllRoomTypes();
		return ResponseEntity.ok(result);
	}
	
	// 回傳頁面資料
	@GetMapping("/roomtype/{roomTypeId}")
	public ResponseEntity<RoomTypeDto> getRoomTypeById(
			@PathVariable Integer roomTypeId
			){
		RoomTypeDto result =stayService.getRoomTypeById(roomTypeId);
		return ResponseEntity.ok(result);
	}
	
	// 建立預約 + LINE Pay
	@PostMapping
	public ResponseEntity<StayPaymentResponseDto> createLineStay(
	        @RequestBody StayRequestDto request) {
	    return ResponseEntity.ok(stayService.createStayWithPayment(request));
	}
	
	// 確認預約
	@GetMapping("/payment/confirm")
	public ResponseEntity<?> confirmPayment(
	        @RequestParam String merchantTradeNo,
	        @RequestParam String transactionId,
	        HttpServletRequest request) {  // ✅ 注入 request
	
	    try {
	        String result = stayService.confirmPayment(merchantTradeNo, transactionId);
	
	        if ("SUCCESS".equals(result)) {
	            Integer stayId = stayService.getStayIdByMerchantTradeNo(merchantTradeNo);
	            
	            // ✅ 從 Referer 取得前端的 origin
	            String referer = request.getHeader("Referer");
	            String origin = referer != null ? 
	                referer.substring(0, referer.indexOf("/", 8)) :
	                "http://localhost:5173";
	            
	            String finalUrl = origin + "/stay/booking-success?stayId=" + stayId;
	
	            return ResponseEntity.status(302)
	                    .header("Location", finalUrl)
	                    .build();
	        } else {
	            // ✅ 失敗也一樣
	            String referer = request.getHeader("Referer");
	            String origin = referer != null ? 
	                referer.substring(0, referer.indexOf("/", 8)) :
	                "http://localhost:5173";
	            
	            return ResponseEntity.status(302)
	                    .header("Location", origin + "/stay")
	                    .build();
	        }
	    } catch (Exception e) {
	        // ✅ 異常也一樣
	        String referer = request.getHeader("Referer");
	        String origin = referer != null ? 
	            referer.substring(0, referer.indexOf("/", 8)) :
	            "http://localhost:5173";
	        
	        return ResponseEntity.status(302)
	                .header("Location", origin + "/stay")
	                .build();
	    }
	}
		
	// 單筆訂單
	@GetMapping("/{stayId}")
	public ResponseEntity<StayResponseDto> getStayById(
	        @PathVariable Integer stayId) {
	    return ResponseEntity.ok(stayService.getStayById(stayId));
	}
	
	// 查所有訂單（後台）
	@GetMapping("/all")
	public ResponseEntity<List<StayResponseDto>> getAllStays() {
	    return ResponseEntity.ok(stayService.getAllStays());
	}
	
	// 依會員姓名搜尋
	@GetMapping("/search/name")
	public ResponseEntity<List<StayResponseDto>> searchByName(
	        @RequestParam String name) {
	    return ResponseEntity.ok(stayService.searchByMemberName(name));
	}

	// 依訂單編號搜尋
	@GetMapping("/search/id")
	public ResponseEntity<List<StayResponseDto>> searchById(
	        @RequestParam Integer stayId) {
	    return ResponseEntity.ok(stayService.searchByStayId(stayId));
	}

	// 依手機末三碼搜尋
	@GetMapping("/search/phone")
	public ResponseEntity<List<StayResponseDto>> searchByPhone(
	        @RequestParam String phone) {
	    return ResponseEntity.ok(stayService.searchByPhone(phone));
	}

	// 修改訂單狀態
	@PatchMapping("/{stayId}/status")
	public ResponseEntity<Void> updateStayStatus(
	        @PathVariable Integer stayId,
	        @RequestParam String status) {
	    stayService.updateStayStatus(stayId, status);
	    return ResponseEntity.ok().build();
	}
	
	// 查所有房間
	@GetMapping("/rooms")
	public ResponseEntity<List<StayRoomDto>> getAllRooms() {
	    return ResponseEntity.ok(stayService.getAllRooms());
	}

	// 修改房間狀態
	@PatchMapping("/rooms/{roomId}/status")
	public ResponseEntity<Void> updateRoomStatus(
	        @PathVariable Integer roomId,
	        @RequestParam String status) {
	    stayService.updateRoomStatus(roomId, status);
	    return ResponseEntity.ok().build();
	}
	
	// 修改房型資訊
	@PutMapping("/roomtype/{roomTypeId}")
	public ResponseEntity<RoomTypeDto> updateRoomType(
	        @PathVariable Integer roomTypeId,
	        @RequestParam Double price,
	        @RequestParam String roomName,
	        @RequestParam Integer capacity,
	        @RequestParam String roomDescription) {
	    return ResponseEntity.ok(
	        stayService.updateRoomType(roomTypeId, price, roomName, capacity, roomDescription));
	}
	
	// 查指定日期房間狀態（新增）
	@GetMapping("/rooms/status")
	public ResponseEntity<List<RoomStatusDto>> getRoomStatusByDate(
	        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
	    return ResponseEntity.ok(stayService.getRoomStatusByDate(date));
	}
	
	}
