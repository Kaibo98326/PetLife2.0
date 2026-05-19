package com.petlife.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.petlife.model.Category;
import com.petlife.model.Product;
import com.petlife.service.CategoryService;
import com.petlife.service.ProductService;

/**
 * 前台商城 API Controller（給 Vue frontend-user 使用）
 * 路徑前綴：/api/shop
 * 
 * 與後台 InnerProductController (/api/products) 完全獨立，互不干擾
 */
@RestController
@RequestMapping("/api/shop")
public class ShopController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private com.petlife.service.SearchKeywordService searchKeywordService;

    // ===== 【商城商品列表】 支援：分頁、每頁筆數、分類篩選、關鍵字搜尋、排序 =============================
    @GetMapping("/products")
    public ResponseEntity<?> listProducts(
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "searchKeyword", defaultValue = "") String keyword,
            @RequestParam(value = "cp", defaultValue = "1") int cp,
            @RequestParam(value = "pageSize", defaultValue = "25") int pageSize,
            @RequestParam(value = "sort", defaultValue = "newest") String sort) {

        // 1. 處理排序邏輯
        org.springframework.data.domain.Sort sortObj = org.springframework.data.domain.Sort.by("productId").descending(); // 預設：最新上架
        if ("price_asc".equals(sort)) {
            sortObj = org.springframework.data.domain.Sort.by("productPrice").ascending();
        } else if ("price_desc".equals(sort)) {
            sortObj = org.springframework.data.domain.Sort.by("productPrice").descending();
        } else if ("newest".equals(sort)) {
            sortObj = org.springframework.data.domain.Sort.by("productId").descending();
        } else if ("sales".equals(sort)) {
            sortObj = org.springframework.data.domain.Sort.by("clickCount").descending();
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(cp - 1, pageSize, sortObj);
        
        Page<Product> productPage;

        if (categoryId != null && categoryId != 0) {
            // ✨ 補回：判斷是否為活動標籤
            Category category = categoryService.getCategoryById(categoryId);
            if (category != null && category.getCategoryType() == 3) {
                // 是活動標籤：走自動跳轉邏輯
                productPage = productService.getProductsByActivityTag(categoryId, pageable);
            } else {
                // 一般分類：走組員新寫的 getActiveProductsByCategory
                productPage = productService.getActiveProductsByCategory(categoryId, pageable);
            }
        } else {
            productPage = productService.searchActiveProducts(keyword.trim(), pageable);
            // ✨ 保留：組員新增的熱門關鍵字紀錄
            if (keyword != null && !keyword.trim().isEmpty()) {
                searchKeywordService.recordKeyword(keyword);
            }
        }
        // 直接取得資料庫回傳的已上架商品
        List<Product> activeProducts = productPage.getContent();
        
        // 手動補齊分類名稱
        for (Product p : activeProducts) {
            if (p.getCategories() != null && !p.getCategories().isEmpty()) {
                String names = p.getCategories().stream()
                        .map(Category::getCategoryName)
                        .collect(java.util.stream.Collectors.joining(", "));
                p.setCategoryName(names);
            }
        }

        // 回傳 JSON 給 Vue 前端
        Map<String, Object> response = new HashMap<>();
        response.put("productList", activeProducts);
        response.put("currentPage", cp);
        response.put("totalPages", productPage.getTotalPages());
        response.put("totalElements", productPage.getTotalElements());

        return ResponseEntity.ok(response);
    }

    // ===== 【熱門排行】 取得點擊率最高的前 5 名 ====================================================
    @GetMapping("/products/top10")
    public ResponseEntity<?> getTop10Products() {
        List<Product> top10 = productService.getTop5HotProducts();
        
        // 手動補齊分類名稱
        for (Product p : top10) {
            if (p.getCategories() != null && !p.getCategories().isEmpty()) {
                String names = p.getCategories().stream()
                        .map(Category::getCategoryName)
                        .collect(java.util.stream.Collectors.joining(", "));
                p.setCategoryName(names);
            }
        }
        
        return ResponseEntity.ok(top10);
    }

//    // ===== 【取得所有分類】 供前台左側選單使用 ====================================================
//    @GetMapping("/categories")
//    public ResponseEntity<?> listCategories() {
//        List<Category> categories = categoryService.getAllCategory();
//        return ResponseEntity.ok(categories);
//    }
    
    @GetMapping("/categories")
    public ResponseEntity<?> listCategories() {
        // ✨ 修改：改為呼叫專供前台使用的查詢，自動過濾掉沒有活動或沒綁定商品的空標籤，但不影響後台
        List<Category> categories = categoryService.getFrontEndCategories();
        return ResponseEntity.ok(categories);
    }

}
    
    
