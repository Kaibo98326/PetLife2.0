package com.petlife.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petlife.model.Member;

public interface MemberRepository extends JpaRepository<Member, Integer> {
	
	//依照Email 找會員
	Optional<Member> findByEmail(String email);
	
	//依照電話找會員
	Optional<Member> findByPhone(String phone);
	
	//模糊搜尋會員姓名
	List<Member> findByMemberNameContaining(String keyword);
}
