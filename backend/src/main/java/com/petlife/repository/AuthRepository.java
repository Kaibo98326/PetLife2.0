package com.petlife.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petlife.model.Auth;

public interface AuthRepository extends JpaRepository<Auth, Integer> {
	
	
	//依 provider + providerUserId 找第三方登入紀錄
	Optional<Auth> findByProviderAndProviderUserId(String provider , String providerUserId);
	
	//依會員找所有綁定紀錄
	List<Auth> findByMemberMemberId(Integer memberId);
	
}
