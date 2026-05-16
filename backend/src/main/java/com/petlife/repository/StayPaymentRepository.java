package com.petlife.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.petlife.model.StayPayment;

@Repository
public interface StayPaymentRepository extends JpaRepository<StayPayment, Integer> {
	
	
	    Optional<StayPayment> findByMerchantTradeNo(String merchantTradeNo);

		Optional<StayPayment> findByStay_StayId(Integer stayId);
		
		@Query("SELECT p FROM StayPayment p WHERE p.stay.stayId = :stayId")
		Optional<StayPayment> findPaymentByStayId(@Param("stayId") Integer stayId);

}
