package com.petlife.controller;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.petlife.model.Member;
import com.petlife.repository.LoginRequest;
import com.petlife.repository.MemberUpdateRequest;
import com.petlife.repository.RegisterRequest;
import com.petlife.service.IMemberService;
import com.petlife.service.JwtUtils;
import com.petlife.service.PasswordUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberController {
	
	@Autowired
	private  IMemberService memberService;
	
	@Autowired
    private JwtUtils JwtUtils;  // ✅ 注入 JwtUtils
	
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
	public ResponseEntity<?> verifyPassword(@RequestBody Map<String,String> body) {
		
		Integer memberId = Integer.valueOf(body.get("memberId"));
		String oldPassword = body.get("oldPassword");
		
	    return memberService.findById(memberId)
	        .map(member -> {
	        		boolean valid = PasswordUtils.checkPassword(oldPassword, member.getPasswordHash());
	        		return ResponseEntity.ok(Map.of("valid", valid));
	        })
	        .orElse(ResponseEntity.ok(Map.of("valid", false)));
	}
	
	
	//會員資料api
	@GetMapping("/me")
	public ResponseEntity<Member> getMyProfile(@RequestHeader("Authorization") String token){
		String jwt = token.replace("Bearer ", "");
		String memberId = JwtUtils.validateToken(jwt);
		
		return memberService.findById(Integer.valueOf(memberId))
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}
	
	//處理大頭貼
	@PostMapping("/{id}/avatar")
	public ResponseEntity<?> uploadAvatar(@PathVariable Integer id ,
										 @RequestParam("file") MultipartFile file){
		try {
			// 檔案存放路徑 (專案內 static/images/member)
			String uploadDir = "C:/PetLife2.0/uploads/images/member/";
			String fileName = id + "_"+System.currentTimeMillis() +"_"+ file.getOriginalFilename();
			Path filePath = Paths.get(uploadDir).resolve(fileName);

            // 建立目錄
            Files.createDirectories(filePath.getParent());

            // 儲存新檔案
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            System.out.println("Saved file to: " + filePath.toAbsolutePath());
            // 更新資料庫欄位 (呼叫 Service 方法，會自動刪除舊檔案)
            String dbPath = "/images/member/" + fileName;
            Member updated = memberService.updateMemberImage(id, dbPath);

            
            return ResponseEntity.ok(Map.of(
                "message", "頭像更新成功",
                "userImage", updated.getUserImage()
            ));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "大頭貼更新失敗", "details", e.getMessage()));
		}
	}
	
	
	
}
