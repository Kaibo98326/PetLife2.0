package com.petlife.controller;

import com.petlife.model.Order;
import com.petlife.service.OrderAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderAdminController {

    @Autowired
    private OrderAdminService oas;

    // 取得所有訂單 (排除已軟刪除的資料)
    @GetMapping("/all")
    public ResponseEntity<List<Order>> getAllOrders(@RequestParam(required = false) String search) {
        // 在 Service 層實作時，務必加上 WHERE is_deleted = 0
        List<Order> orders = oas.findActiveOrders(search);
        return ResponseEntity.ok(orders);
    }

    // 取得訂單詳細明細(含商品列表)
    @GetMapping("/detail/{id}")
    public ResponseEntity<Map<String, Object>> getOrderDetail(@PathVariable Integer id) {
        Map<String, Object> detailMap = oas.getOrderWithDetails(id);
        return ResponseEntity.ok(detailMap);
    }

    // 儲存/更新訂單狀態與付款方式
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateOrder(@PathVariable Integer id, @RequestBody Map<String, String> payload) {
        try {
            String status = payload.get("orderStatus");
            String payment = payload.get("orderPayment");
            
            oas.updateStatusAndPayment(id, status, payment);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("更新失敗: " + e.getMessage());
        }
    }

    // 刪除訂單(軟刪)
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> softDeleteOrder(@PathVariable Integer id) {
        try {
            boolean success = oas.performSoftDelete(id);
            
            if (success) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(404).body("找不到訂單");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("伺服器處理失敗");
        }
    }
}