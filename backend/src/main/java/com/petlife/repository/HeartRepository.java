package com.petlife.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petlife.model.Heart;

public interface HeartRepository extends JpaRepository<Heart, Integer>{

	// 檢查是否已收藏
	boolean existsByMemberIdAndProductId(Integer memberId,Integer productId);
	
	List<Heart> findByMemberIdAndProductId(Integer memberId ,Integer productId);
	
	// 找出有收藏的
	List<Heart> findByMemberId(Integer memberId);
	
	// 取消收藏
	void deleteByMemberIdAndProductId(Integer memberId, Integer productId);
	
}
