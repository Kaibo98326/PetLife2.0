package com.petlife.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.petlife.model.Category;
import com.petlife.model.Product;
import com.petlife.service.CategoryService;
import com.petlife.service.ProductService;

@RestController 
@RequestMapping("/api/products") 
public class InnerProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    //===== 查詢商品列表 (支援分頁、搜尋、分類篩選) ===================================================
    @GetMapping("/list")
    public ResponseEntity<?> list(
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "searchKeyword", defaultValue = "") String keyword,
            @RequestParam(value = "cp", defaultValue = "1") int cp,
            @RequestParam(value = "lowStock", defaultValue = "false") boolean lowStock) {
        
        int pageSize = 10;
        Page<Product> productPage;

        if (lowStock) {
            productPage = productService.getLowStockProducts(cp, pageSize);
        } else if (categoryId != null && categoryId != 0) {
            productPage = productService.getProductsByCategory(categoryId, cp, pageSize);
        } else {
            productPage = productService.searchProducts(keyword, cp, pageSize);
        }

        List<Product> productList = productPage.getContent();
        // 手動補齊分類名稱 (這段邏輯保留)
        for (Product p : productList) {
            if (p.getCategoryId() != null) {
                Category cat = categoryService.getCategoryById(p.getCategoryId());
                if (cat != null) p.setCategoryName(cat.getCategoryName());
            }
        }

        // 把原本丟給 Model 的東西，包成一個 Map 回傳給 Vue
        Map<String, Object> response = new HashMap<>();
        response.put("productList", productList);
        response.put("currentPage", cp);
        response.put("totalPages", productPage.getTotalPages());
        response.put("totalElements", productPage.getTotalElements());
        response.put("lowStockCount", productService.getLowStockCount());

        return ResponseEntity.ok(response);
    }

    //===== 新增商品 ===========================================================================
    @PostMapping("/insert")
    public ResponseEntity<?> insertProduct(
            @ModelAttribute Product product, // 使用 ModelAttribute 接收 Form Data
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            handleImageUpload(product, file, "default_product.jpg");
            productService.addProduct(product);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("fail: " + e.getMessage());
        }
    }

    //===== 修改商品 ===========================================================================
    @PutMapping("/update") // 建議用 PUT
    public ResponseEntity<?> updateProduct(
            @ModelAttribute Product product,
            @RequestParam(value = "oldImage", required = false) String oldImage,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            handleImageUpload(product, file, oldImage);
            productService.updateProduct(product);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("fail");
        }
    }

    //===== 刪除商品 ===========================================================================
    @DeleteMapping("/delete/{id}") // 建議用 DELETE
    public ResponseEntity<?> delete(@PathVariable("id") Integer id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("fail");
        }
    }

    //===== 取得單一商品詳情 =====================================================================
    @GetMapping("/detail/{id}")
    public ResponseEntity<?> showDetail(@PathVariable("id") Integer id) {
        Product product = productService.getProductById(id);
        if (product != null && product.getCategoryId() != null) {
            Category cat = categoryService.getCategoryById(product.getCategoryId());
            if (cat != null) product.setCategoryName(cat.getCategoryName());
        }
        return ResponseEntity.ok(product);
    }

    //===== 圖片處理工具 (邏輯不變) ===============================================================
    private void handleImageUpload(Product product, MultipartFile file, String defaultImage) throws Exception {
        String uploadDir = "C:/uploads/images/products/";
        java.io.File directory = new java.io.File(uploadDir);
        if (!directory.exists()) directory.mkdirs();

        if (file != null && !file.isEmpty()) {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename(); // 加上時間戳防檔名重複
            java.io.File saveFile = new java.io.File(directory, fileName);
            file.transferTo(saveFile);
            product.setProductImage("images/products/" + fileName);
        } else {
            product.setProductImage(defaultImage);
        }
    }
}