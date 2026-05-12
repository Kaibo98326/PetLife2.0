package com.petlife.repository;

import com.petlife.model.OrderDiscount;
import com.petlife.model.OrderDiscountId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDiscountRepository extends JpaRepository<OrderDiscount, OrderDiscountId> {
    
    // 透過訂單編號查詢該訂單所有的折扣紀錄 (圖1管理介面用得到)
    List<OrderDiscount> findByOrderId(Integer orderId);
    
    // 透過活動編號查詢有哪些訂單使用了這個折扣 (數據統計用)
    List<OrderDiscount> findByDiscountId(Integer discountId);
}