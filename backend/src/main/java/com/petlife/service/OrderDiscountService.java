package com.petlife.service;

import com.petlife.model.OrderDiscount;
import com.petlife.repository.OrderDiscountRepository;
import com.petlife.repository.OrderDiscountSummaryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrderDiscountService {

    @Autowired
    private OrderDiscountRepository orderDiscountRepository;

    // 1. 批量儲存訂單折扣紀錄 (結帳時呼叫)
    public List<OrderDiscount> saveAllOrderDiscounts(List<OrderDiscount> discounts) {
        return orderDiscountRepository.saveAll(discounts);
    }

    // 2. 儲存單筆訂單折扣紀錄
    public OrderDiscount saveOrderDiscount(OrderDiscount discount) {
        return orderDiscountRepository.save(discount);
    }

    // 3. 根據訂單ID查詢折扣明細 (後台訂單管理 / 圖1活動成效查看用)
    public List<OrderDiscount> getDiscountsByOrderId(Integer orderId) {
        return orderDiscountRepository.findByOrderId(orderId);
    }
    
    // 透過活動 ID 查詢該活動被哪些訂單使用 (給後台明細按鈕用)
    public List<OrderDiscount> getDiscountsByDiscountId(Integer discountId) {
        return orderDiscountRepository.findByDiscountId(discountId);
    }

    // 活動修改：回傳含活動名稱的摘要，供結帳成功頁顯示折抵明細（不需重新計算）
    public List<OrderDiscountSummaryDTO> getDiscountSummaryByOrderId(Integer orderId) {
        return orderDiscountRepository.findSummaryByOrderId(orderId);
    }
}