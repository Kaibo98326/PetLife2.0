package com.petlife.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.petlife.model.Member;

public interface MemberRepository extends JpaRepository<Member, Integer> {
	
	//依照Email 找會員
	Optional<Member> findByEmail(String email);
	
	//依照電話找會員
	Optional<Member> findByPhone(String phone);
	
	//模糊搜尋會員姓名
	List<Member> findByMemberNameContaining(String keyword);
	
	//以下為員工端使用
	//分頁形式(模糊搜尋姓名)
	Page<Member> findByMemberNameContaining(String keyword, Pageable pageable);
	//分頁形式(模糊搜尋eamil)
	Page<Member> findByEmailContaining(String keyword, Pageable pageable);
	//分頁形式(模糊搜尋會員末三碼)
	Page<Member> findByPhoneEndingWith(String keyword, Pageable pageable);
	//分頁形式(搜尋帳號狀態)
	Page<Member> findByAccountStatus(String accountStatus, Pageable pageable);
	//分頁形式(搜尋是否為第三方登入帳號)
	Page<Member> findByProvider(String provider, Pageable pageable);
	//分頁形式(搜尋全部會員)
	Page<Member> findAll(Pageable pageable);
	//分頁形式(搜尋本地端會員)
	Page<Member> findByProviderIsNull(Pageable pageable);
}
