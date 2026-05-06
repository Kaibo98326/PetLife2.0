package com.petlife.controller;

import com.petlife.repository.StayRepository;
import com.petlife.repository.StayRequestDto;
import com.petlife.repository.StayResponseDto;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.petlife.repository.RoomTypeDto;
import com.petlife.service.IStayService;

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
	
	// 建立預約
	@PostMapping
	public ResponseEntity<StayResponseDto> createStay(@RequestBody StayRequestDto request){
		StayResponseDto result = stayService.createStay(request);
		return ResponseEntity.ok(result);
	}
	
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
}
