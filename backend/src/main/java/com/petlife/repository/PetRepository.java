package com.petlife.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.petlife.model.Pet;

@Repository
public interface PetRepository extends JpaRepository<Pet, Integer> {

	//查某會員的所有寵物
	List<Pet> findByMember_MemberId(Integer memberId); 
}
