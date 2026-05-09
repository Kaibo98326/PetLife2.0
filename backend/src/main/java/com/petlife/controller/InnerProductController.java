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
        for (Product p : productList) {
            if (p.getCategories() != null && !p.getCategories().isEmpty()) {
                String names = p.getCategories().stream()
                        .map(Category::getCategoryName)
                        .collect(java.util.stream.Collectors.joining(", "));
                p.setCategoryName(names);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("productList", productList);
        response.put("currentPage", cp);
        response.put("totalPages", productPage.getTotalPages());
        response.put("totalElements", productPage.getTotalElements());
        response.put("lowStockCount", productService.getLowStockCount());

        return ResponseEntity.ok(response);
    }

    //===== 新增商品 (向下相容：保留 file, 新增 extraFiles) ==========================================================
    @PostMapping("/insert")
    public ResponseEntity<?> insertProduct(
            @ModelAttribute Product product,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "extraFiles", required = false) MultipartFile[] extraFiles) {
        try {
            handleMultiImageUpload(product, file, extraFiles);
            productService.addProduct(product);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("fail: " + e.getMessage());
        }
    }

    //===== 修改商品 (向下相容：保留 oldImage, file, 新增 extraFiles) =================================================
    @PostMapping("/update")
    public ResponseEntity<?> updateProduct(
            @ModelAttribute Product product,
            @RequestParam(value = "oldImage", required = false) String oldImage,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "extraFiles", required = false) MultipartFile[] extraFiles) {
        try {
            // 如果沒有新傳主圖，就保留舊主圖路徑
            if (file == null || file.isEmpty()) {
                product.setProductImage(oldImage);
            }
            handleMultiImageUpload(product, file, extraFiles);
            productService.updateProduct(product);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("fail");
        }
    }

    //===== 刪除商品 ===========================================================================
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Integer id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("fail");
        }
    }

    //===== 批次更新商品狀態 =====================================================================
    @PostMapping("/batchUpdateStatus")
    public ResponseEntity<?> batchUpdateStatus(@RequestBody Map<String, Object> payload) {
        try {
            List<Integer> ids = (List<Integer>) payload.get("ids");
            Integer status = Integer.valueOf(payload.get("status").toString());
            
            if (ids != null && !ids.isEmpty() && status != null) {
                productService.batchUpdateStatus(ids, status);
                return ResponseEntity.ok("success");
            }
            return ResponseEntity.badRequest().body("invalid parameters");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("fail");
        }
    }

    //===== 取得單一商品詳情 =====================================================================
    @GetMapping("/detail/{id}")
    public ResponseEntity<?> showDetail(@PathVariable("id") Integer id) {
        Product product = productService.getProductById(id);
        if (product != null && product.getCategories() != null && !product.getCategories().isEmpty()) {
            String names = product.getCategories().stream()
                    .map(Category::getCategoryName)
                    .collect(java.util.stream.Collectors.joining(", "));
            product.setCategoryName(names);
        }
        return ResponseEntity.ok(product);
    }

    //===== 圖片處理工具 (支援單主圖 + 多張細節圖) ===============================================================
    private void handleMultiImageUpload(Product product, MultipartFile mainFile, MultipartFile[] extraFiles) throws Exception {
        String uploadDir = "C:/PetLife2.0/uploads/images/products/";
        java.io.File directory = new java.io.File(uploadDir);
        if (!directory.exists()) directory.mkdirs();

        // 1. 處理主圖 (原本的邏輯)
        if (mainFile != null && !mainFile.isEmpty()) {
            String safeFileName = sanitizeFileName(mainFile.getOriginalFilename());
            String fileName = System.currentTimeMillis() + "_main_" + safeFileName;
            java.io.File saveFile = new java.io.File(directory, fileName);
            mainFile.transferTo(saveFile);
            product.setProductImage("images/products/" + fileName);
        }

        // 2. 處理額外多圖
        if (extraFiles != null && extraFiles.length > 0) {
            java.util.List<com.petlife.model.ProductImage> imageList = new java.util.ArrayList<>();
            
            for (int i = 0; i < extraFiles.length; i++) {
                MultipartFile file = extraFiles[i];
                if (file.isEmpty()) continue;

                String safeFileName = sanitizeFileName(file.getOriginalFilename());
                String fileName = System.currentTimeMillis() + "_extra_" + i + "_" + safeFileName;
                java.io.File saveFile = new java.io.File(directory, fileName);
                file.transferTo(saveFile);
                
                String relativePath = "images/products/" + fileName;
                
                com.petlife.model.ProductImage pi = new com.petlife.model.ProductImage();
                pi.setImageUrl(relativePath);
                pi.setProduct(product);
                pi.setSortOrder(i);
                pi.setCreatedAt(java.time.LocalDateTime.now());
                imageList.add(pi);
            }
            
            if (!imageList.isEmpty()) {
                product.setImages(imageList);
            }
        }
        
        // 3. 如果沒有主圖，給予預設圖
        if (product.getProductImage() == null || product.getProductImage().trim().isEmpty()) {
            product.setProductImage("images/products/default.jpg");
        }
    }

    // 檔名消毒處理，避免特殊字元(例如 %、空白等)導致前端 URL 請求失敗 (Tomcat 400 -> CORB 錯誤)
    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "unknown.jpg";
        // 將非英數字、點、減號的字元(包含中文、空白、特殊符號等)替換為底線
        return fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
    }
}