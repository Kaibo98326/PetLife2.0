package com.petlife.repository;

import lombok.Data;

@Data
public class UpdateMemberRequest {
	
	private String memberName;
	
	private String phone;
	
	private String address;
	
	private String accountStatus;
}
