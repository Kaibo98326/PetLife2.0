package com.petlife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.petlife.model.Discount;


public interface DiscountRepository extends JpaRepository<Discount, Integer> {
	// 基礎的 save, findAll, deleteById 已由 JpaRepository 提供
}