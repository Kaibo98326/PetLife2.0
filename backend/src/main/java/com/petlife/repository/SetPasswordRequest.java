package com.petlife.repository;

import lombok.Data;

@Data
public class SetPasswordRequest {
	
	private Integer memberId;
	
	private String newPassword;
	
	
}
