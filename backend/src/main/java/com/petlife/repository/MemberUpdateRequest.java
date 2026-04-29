package com.petlife.repository;

import lombok.Data;

@Data
public class MemberUpdateRequest {
	
	private Integer memberId;
    private String phone;
    private String email;
    private String address;
    private String password; // 新密碼
    private String provider; 
    private String providerUserId; 
	
    private String userImage;      // 新增：使用者大頭像
    private Integer bonusPoints;   // 新增：紅利點數（預設 0，不可為負）
}
