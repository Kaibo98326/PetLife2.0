package com.petlife.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.petlife.model.Order;
import com.petlife.service.OrderService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	@Autowired
	private OrderService orderService;

	@PostMapping("/checkout")
	public ResponseEntity<Map<String, Object>> checkout(@RequestBody Order order, @RequestParam Integer cartId) {

		// Service(存訂單、搬明細、清購物車、算加密)
		Map<String, Object> result = orderService.processCheckoutAndReturnDetail(order, cartId);

		// 2. 回傳給 Vue
		return ResponseEntity.ok(result);
	}

	@GetMapping("/detail/{orderId}")
	public ResponseEntity<Map<String, Object>> getOrderDetail(@PathVariable Integer orderId) {
		Map<String, Object> orderDetail = orderService.getOrderDetailWithItems(orderId);

		if (orderDetail != null) {
			return ResponseEntity.ok(orderDetail);
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	// 歷史訂單
    @GetMapping("/historyorders")
    public ResponseEntity<?> getMyOrders(HttpSession session) {
        Integer memberId = (Integer) session.getAttribute("memberId");
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("請先登入");
        }
        
        List<Order> orders = orderService.findByMemberId(memberId);
        return ResponseEntity.ok(orders);
    }
    
}