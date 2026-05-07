package com.petlife.controller;



import com.petlife.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petlife.repository.RegisterRequest;
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
	
	
	
	
	
	
}
