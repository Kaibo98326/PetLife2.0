package com.petlife.controller;

import com.petlife.model.Order;
import com.petlife.service.OrderAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.petlife.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderAdminController {

    @Autowired
    private OrderAdminService oas;

    // 為了呼叫帶有扣點/發點機制
    @Autowired
    private OrderService orderService;

    // 取得所有訂單 (排除已軟刪除的資料)
    @GetMapping("/all")
    public ResponseEntity<List<Order>> getAllOrders(@RequestParam(required = false) String search) {
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

            // ✨ 新增/修改：攔截狀態變更，強制呼叫包含紅利點數處理的原子化 Service 方法
            if ("已完成".equals(status)) {
                // 呼叫前台 Service，執行發放點數並更新狀態為"已完成"
                orderService.completeOrder(id);
            } else if ("已取消".equals(status)) {
                // 呼叫前台 Service，執行退回點數並更新狀態為"已取消"
                orderService.cancelOrder(id);
            }

            // 繼續執行原本的更新邏輯，確保「付款方式」等其他欄位也能正常被更新存檔
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

    // 結帳方式分析
    @GetMapping("/analysis/payment")
    public ResponseEntity<Map<String, Long>> getPaymentAnalysis() {
        return ResponseEntity.ok(oas.getPaymentAnalysis());
    }

    // 近一個月狀態分析
    @GetMapping("/analysis/status")
    public ResponseEntity<Map<String, Long>> getRecentStatusAnalysis() {
        return ResponseEntity.ok(oas.getRecentStatusAnalysis());
    }

    // 取得每月訂單趨勢分析
    @GetMapping("/analysis/trends")
    public ResponseEntity<List<Map<String, Object>>> getOrderTrendAnalysis() {
        return ResponseEntity.ok(oas.getOrderTrendAnalysis());
    }

    // 點擊圖表時動態載入對應的訂單列表
    @GetMapping("/analysis/listbycondition")
    public ResponseEntity<List<Order>> getOrdersByCondition(
            @RequestParam String searchType,
            @RequestParam String keyword) {
        List<Order> orders = oas.findOrdersByCondition(searchType, keyword);
        return ResponseEntity.ok(orders);
    }
}