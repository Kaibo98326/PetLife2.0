package com.petlife.controller;

import com.petlife.service.ProductViewHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class ProductViewHistoryController {

    @Autowired
    private ProductViewHistoryService historyService;

    /**
     * 記錄商品瀏覽
     */
    @PostMapping("/record")
    public ResponseEntity<?> recordView(@RequestBody Map<String, Integer> payload) {
        Integer memberId = payload.get("memberId");
        Integer productId = payload.get("productId");

        if (memberId != null && productId != null) {
            historyService.recordView(memberId, productId);
            return ResponseEntity.ok("recorded");
        }
        return ResponseEntity.badRequest().body("missing memberId or productId");
    }

    /**
     * 取得最近瀏覽紀錄
     */
    @GetMapping("/{memberId}")
    public ResponseEntity<List<Map<String, Object>>> getRecentViews(@PathVariable Integer memberId) {
        List<Map<String, Object>> products = historyService.getRecentViews(memberId);
        return ResponseEntity.ok(products);
    }
}
