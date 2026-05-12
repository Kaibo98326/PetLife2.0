package com.petlife.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petlife.model.Heart;
import com.petlife.service.HeartService;

@RestController
@RequestMapping("/api/heart")
public class HeartController {

    @Autowired
    private HeartService hs;

    // 取得會員的所有追蹤清單
    @GetMapping("/list")
    public ResponseEntity<List<Heart>> getWatchList(@RequestParam Integer memberId) {
        List<Heart> watchList = hs.getWatchListWithComparison(memberId);
        return ResponseEntity.ok(watchList);
    }

    // 切換收藏狀態
    @PostMapping("/toggle")
    public ResponseEntity<String> toggleHeart(
            @RequestParam Integer memberId, 
            @RequestParam Integer productId) {
        String message = hs.toggleHeart(memberId, productId);
        return ResponseEntity.ok(message);
    }

    // 刪除收藏
    @DeleteMapping("/remove")
    public ResponseEntity<Void> removeHeart(
            @RequestParam Integer memberId, 
            @RequestParam Integer productId) {
        hs.removeHeart(memberId, productId);
        return ResponseEntity.noContent().build();
    }
}
