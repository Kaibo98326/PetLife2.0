package com.petlife.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.petlife.model.Order;
import com.petlife.service.OrderService;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*") 
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
}
