package com.petlife.controller;

import com.petlife.model.OrderDiscount;
import com.petlife.service.OrderDiscountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-discounts")
public class OrderDiscountController {

    @Autowired
    private OrderDiscountService orderDiscountService;

    /**
     * 查詢某筆訂單的所有折扣明細
     * (這可以做在圖1後台，點擊查看活動使用情況時呼叫)
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<OrderDiscount>> getDiscountsByOrderId(@PathVariable Integer orderId) {
        List<OrderDiscount> discounts = orderDiscountService.getDiscountsByOrderId(orderId);
        return ResponseEntity.ok(discounts);
    }

    /**
     * 手動/測試儲存訂單折扣明細
     */
    @PostMapping("/save")
    public ResponseEntity<String> saveOrderDiscounts(@RequestBody List<OrderDiscount> discounts) {
        orderDiscountService.saveAllOrderDiscounts(discounts);
        return ResponseEntity.ok("訂單折扣紀錄儲存成功");
    }
    
    /**
     * 查詢某個活動被哪些訂單使用 (後台活動明細用)
     */
    @GetMapping("/discount/{discountId}")
    public ResponseEntity<List<OrderDiscount>> getDiscountsByDiscountId(@PathVariable Integer discountId) {
        List<OrderDiscount> discounts = orderDiscountService.getDiscountsByDiscountId(discountId);
        return ResponseEntity.ok(discounts);
    }
}