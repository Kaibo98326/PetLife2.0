package com.petlife.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.petlife.model.StayPayment;

@Repository
public interface StayPaymentRepository extends JpaRepository<StayPayment, Integer> {
	
	
	    Optional<StayPayment> findByMerchantTradeNo(String merchantTradeNo);

}
