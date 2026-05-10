package com.petlife.repository;

import lombok.Data;

@Data
public class GoogleUserInfo {
	private String sub;   	//Google 唯一ID
	private String email;	//使用者email
	private String name;		//使用者名稱
	private String picture;	//使用者大頭貼
}
