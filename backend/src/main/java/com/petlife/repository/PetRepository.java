package com.petlife.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petlife.model.Pet;

public interface PetRepository extends JpaRepository<Pet, Integer> {
	
	//會員端可以查詢所有未刪除寵物
	List<Pet> findByMemberIdAndStatus(Integer memberId,String status);
}
