package com.petlife.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "PasswordResetToken")
@Data
public class PasswordResetToken {
	
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "token_id")
	private Integer tokenId;
	
	@ManyToOne
	@JoinColumn(name = "member_id",nullable = false)
	private Member member;
	
	@Column(name = "token",nullable = false,unique = true)
	private String token;
	
	@Column(name = "expire_at",nullable = false)
	private LocalDateTime expireAt;
	
	@Column(name = "used", nullable =  false)
	private Boolean used = false ;
	
	@Column(name = "created_at")
	private LocalDateTime createdAt;
	
}
