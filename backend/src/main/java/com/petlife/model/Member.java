package com.petlife.model;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity @Table(name = "Member")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "member_id")
	private Integer memberId;	//會員編號
	

	@Column(name = "member_name" ,nullable = false)
	private String memberName;	//會員姓名
	
	private String phone;		//電話
	
	@Column(nullable = false ,unique = true)
	private String email;		//登入帳號(e-mail)
	

	@Column(name = "password_hash",nullable = false)
	@JsonIgnore
	private String passwordHash; //雜湊密碼
	
	private String address;
	private String provider;


	@Column(name = "provider_user_id")
	private String providerUserId;

	@Column(name = "account_status")
	private String accountStatus;
	
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@Column(name = "register_time")
	private LocalDateTime registerTime;
	
	@Column(name = "last_login_at")
	private LocalDateTime lastLogin;
	
}
