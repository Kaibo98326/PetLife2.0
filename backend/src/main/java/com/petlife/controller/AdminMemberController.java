//package com.petlife.controller;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.petlife.model.Member;
//import com.petlife.service.IMemberService;
//
//import lombok.RequiredArgsConstructor;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/admin/member")
//public class AdminMemberController {
//	
//	
//	private final IMemberService memberService;
//	
//	//管理員新增會員
//	@PostMapping("add")
//	public ResponseEntity<Member> addMember(@RequestBody Member member){
//		//可以額外設定帳號狀態等
//		member.setAccountStatus("active");
//		Member saved = memberService.register(member);
//		
//		return ResponseEntity.ok(saved);
//	}
//	
//}
