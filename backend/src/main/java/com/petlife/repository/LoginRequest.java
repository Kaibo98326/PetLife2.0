package com.petlife.repository;

import lombok.Data;

@Data
public class LoginRequest {
	//接收使用者前端傳來的登入JSON
	private String email;
	private String password;
}
