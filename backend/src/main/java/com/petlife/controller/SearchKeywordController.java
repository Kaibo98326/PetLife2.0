package com.petlife.controller;

import com.petlife.model.SearchKeyword;
import com.petlife.service.SearchKeywordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop/search")
public class SearchKeywordController {

    @Autowired
    private SearchKeywordService searchKeywordService;

    /**
     * 取得熱門關鍵字
     */
    @GetMapping("/hot")
    public ResponseEntity<List<SearchKeyword>> getHotKeywords() {
        return ResponseEntity.ok(searchKeywordService.getHotKeywords());
    }

    /**
     * 手動紀錄關鍵字 (供前端調用)
     */
    @PostMapping("/record")
    public ResponseEntity<?> recordKeyword(@RequestParam String keyword) {
        searchKeywordService.recordKeyword(keyword);
        return ResponseEntity.ok().build();
    }
}
