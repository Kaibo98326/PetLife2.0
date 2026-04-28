package com.petlife.repository;

import lombok.Data;

@Data
public class MemberUpdateRequest {
	
	private Integer memberId;
    private String phone;
    private String email;
    private String address;
    private String password; // 新密碼（可選）
    private String provider; // 可選
    private String providerUserId; // 可選
	
	
}
