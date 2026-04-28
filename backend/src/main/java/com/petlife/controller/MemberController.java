package com.petlife.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petlife.model.Member;
import com.petlife.repository.LoginRequest;
import com.petlife.repository.MemberUpdateRequest;
import com.petlife.repository.RegisterRequest;
import com.petlife.service.IMemberService;
import com.petlife.service.PasswordUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberController {
	
	@Autowired
	private  IMemberService memberService;
	
	
	
	//會員自己註冊
	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody RegisterRequest request){
		try {
			Member member = memberService.register(request);
			return ResponseEntity.ok(member);			
			
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
		
	}
	
	//會員登入
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequest request){
		
		return memberService.loginAndGenerateToken(request)
				.map( token -> ResponseEntity.ok(token))
				.orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("帳號或密碼錯誤"));
	}
	@GetMapping("/{id}")
	public ResponseEntity<Member> getMemberById(@PathVariable Integer id) {
		return  memberService.findById(id)
						.map(ResponseEntity::ok)
						.orElse(ResponseEntity.notFound().build());
	    
	}
	
	@GetMapping("/checkEmail")
	public Map<String, Boolean> checkEmail(@RequestParam String email) {
	    boolean available = memberService.isEmailAvailable(email);
	    return Map.of("available", available);
	}

	@GetMapping("/checkPhone")
	public Map<String, Boolean> checkPhone(@RequestParam String phone) {
	    boolean available = memberService.isPhoneAvailable(phone);
	    return Map.of("available", available);
	}
	
	//會員修改資料
	@PostMapping("/update")
	public ResponseEntity<Member> updateMember(@RequestBody MemberUpdateRequest request) {
        Member updated = memberService.updateMember(request);
        return ResponseEntity.ok(updated);
    }
	
	//修改密碼前驗證舊密碼
	@PostMapping("/verifyPassword")
	public ResponseEntity<?> verifyPassword(@RequestParam Integer memberId, @RequestParam String oldPassword) {
	    return memberService.findById(memberId)
	        .filter(m -> PasswordUtils.checkPassword(oldPassword, m.getPasswordHash()))
	        .map(m -> ResponseEntity.ok(Map.of("valid", true)))
	        .orElse(ResponseEntity.ok(Map.of("valid", false)));
	}
	
}
