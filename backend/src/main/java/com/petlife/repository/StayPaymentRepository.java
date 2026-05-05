package com.petlife.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petlife.model.StayPayment;

public interface StayPaymentRepository extends JpaRepository<StayPayment, Integer> {

}
