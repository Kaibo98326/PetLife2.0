package com.petlife.controller;



import com.petlife.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petlife.repository.RegisterRequest;
import com.petlife.repository.UpdateMemberRequest;
import com.petlife.repository.UpdateMemberStatusRequest;
import com.petlife.service.AdminMemberService;
import com.petlife.service.IMemberService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members")
public class AdminMemberController {
	
	
	private final IMemberService memberService;
	private final AdminMemberService adminMemberService;

	
	
	//搜尋現有會員(分頁方式)
	@GetMapping
	public ResponseEntity<?> getMember(@RequestParam(defaultValue = "0") int page,
									   @RequestParam(defaultValue = "10") int size,
									   @RequestParam(required = false) String searchType,
									   @RequestParam(required = false) String keyword){
		
		return ResponseEntity.ok(adminMemberService.getMembers(page, size, searchType, keyword));
	}
	
	//員工端新增會員
	@PostMapping
	public ResponseEntity<?> addMember(@RequestBody RegisterRequest request){
		
		return ResponseEntity.ok(memberService.register(request));
	}
	
	//員工端快速修改會員狀態攔
	@PutMapping("/{memberId}/status")
	public ResponseEntity<?>  updateMemberStatus(@PathVariable Integer memberId,
												@RequestBody UpdateMemberStatusRequest request){
		
		adminMemberService.updateMemberStatus(memberId, request.getAccountStatus());
		
		return ResponseEntity.ok("狀態更新成功");
	}
	
	//員工端修改會員資料
	@PutMapping("/{memberId}")
	public ResponseEntity<?> updateMember(@PathVariable Integer memberId,
										 @RequestBody UpdateMemberRequest request){
		adminMemberService.updateMember(memberId, request);
		
		return ResponseEntity.ok("會員資料更新成功");
	}
	
	
	//員工端顯示會員狀態圓餅圖
	@GetMapping("/analysis/status")
	public ResponseEntity<?> getMemberStatusAnalysis(){
		
		return ResponseEntity.ok(adminMemberService.getMemberStatusAnalysis());
	}
	//員工端顯示會員登入來源
	@GetMapping("/analysis/provider")
	public ResponseEntity<?> getProviderAnalysis(){
		
		return ResponseEntity.ok(adminMemberService.getProviderAnalysis());
	}
	//員工端顯示會員每月註冊趨勢
	@GetMapping("/analysis/register-trend")
	public ResponseEntity<?> getMonthlyRegisterStats() {
		return ResponseEntity.ok(adminMemberService.getMonthlyRegisterStats());
	}
	//月份切換
	@GetMapping("/register-month")
	public ResponseEntity<?> getMembersByRegisterMonth(@RequestParam String month){
		
		return ResponseEntity.ok(adminMemberService.getMembersByRegisterMonth(month));
	}
	
	
	
	
	
}
