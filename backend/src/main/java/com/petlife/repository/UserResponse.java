package com.petlife.repository;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {
	
	private Integer memberId;

    private String memberName;

    private String email;

    private String userImage;
}
