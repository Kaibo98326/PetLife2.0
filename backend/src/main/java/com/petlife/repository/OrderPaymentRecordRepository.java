package com.petlife.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petlife.model.OrderPaymentRecord;

public interface OrderPaymentRecordRepository extends JpaRepository<OrderPaymentRecord, Integer>{
	
	// 當綠界回傳通知會用MerchantTradeNo來找紀錄
    Optional<OrderPaymentRecord> findByMerchantTradeNo(String merchantTradeNo);
    
    // 用來檢查某個訂單是否已經有支付成功的紀錄
    boolean existsByOrderOrderIdAndPaymentStatus(Integer orderId, String status);
}
