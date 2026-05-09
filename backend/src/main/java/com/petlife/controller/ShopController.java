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
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(cp - 1, pageSize, sortObj);
        
        Page<Product> productPage;

        if (categoryId != null && categoryId != 0) {
            // 【分類篩選】(僅查上架)
            productPage = productService.getActiveProductsByCategory(categoryId, pageable);
        } else {
            // 【關鍵字搜尋 / 全部商品】(僅查上架)
            productPage = productService.searchActiveProducts(keyword.trim(), pageable);
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

    // ===== 【取得所有分類】 供前台左側選單使用 ====================================================
    @GetMapping("/categories")
    public ResponseEntity<?> listCategories() {
        List<Category> categories = categoryService.getAllCategory();
        return ResponseEntity.ok(categories);
    }
}
