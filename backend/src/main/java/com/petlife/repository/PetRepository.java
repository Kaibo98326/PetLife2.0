package com.petlife.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.petlife.model.Pet;

public interface PetRepository extends JpaRepository<Pet, Integer> {
	
	//會員端可以查詢所有未刪除寵物
	List<Pet> findByMember_MemberIdAndStatus(Integer memberId,String status);
	
	Page<Pet> findByPetNameContaining(String petName, Pageable pageable);

	Page<Pet> findBySpecies(String species, Pageable pageable);

	Page<Pet> findByStatus(String status, Pageable pageable);

	Page<Pet> findByMember_MemberId(Integer memberId, Pageable pageable);
	
	List<Pet> findByMember_MemberId(Integer memberId); 
}
