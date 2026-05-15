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
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "minStock", required = false) Integer minStock,
            @RequestParam(value = "maxStock", required = false) Integer maxStock,
            @RequestParam(value = "cp", defaultValue = "1") int cp,
            @RequestParam(value = "ps", defaultValue = "10") int ps,
            @RequestParam(value = "lowStock", defaultValue = "false") boolean lowStock) {
        
        Page<Product> productPage = productService.getCompositeProducts(
                keyword, categoryId, status, minPrice, maxPrice, minStock, maxStock, lowStock, cp, ps);

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

    //===== 取得低庫存預警總數 (專供 Store 刷新使用) ===================================================
    @GetMapping("/low-stock-count")
    public ResponseEntity<?> getLowStockCount() {
        return ResponseEntity.ok(productService.getLowStockCount());
    }

    //===== 新增商品 (向下相容：保留 file, 新增 extraFiles) ==========================================================
    @PostMapping("/insert")
    public ResponseEntity<?> insertProduct(
            @ModelAttribute Product product,
            @RequestParam(value = "categoryIds", required = false) List<Integer> categoryIds,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "extraFiles", required = false) MultipartFile[] extraFiles) {
        try {
            if (categoryIds != null) {
                product.setCategoryIds(categoryIds);
            }
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
            @RequestParam(value = "categoryIds", required = false) List<Integer> categoryIds,
            @RequestParam(value = "oldImage", required = false) String oldImage,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "extraFiles", required = false) MultipartFile[] extraFiles) {
        try {
            // 先從資料庫取出原始物件
            Product existingProduct = productService.getProductById(product.getProductId());
            if (existingProduct == null) {
                return ResponseEntity.status(404).body("Product not found");
            }

            // 處理分類：只有在真的有傳送新的 categoryIds 時才更新關聯
            if (categoryIds != null) {
                existingProduct.setCategoryIds(categoryIds);
            } else {
                // 如果前端沒傳 categoryIds，我們絕對不準動到現有的 categories
                // 這裡什麼都不做，保留 existingProduct 原本的 categories
            }

            // 更新其他基本欄位
            existingProduct.setProductName(product.getProductName());
            existingProduct.setProductPrice(product.getProductPrice());
            existingProduct.setProductStock(product.getProductStock());
            existingProduct.setLowStock(product.getLowStock()); // 補上這行
            existingProduct.setStoragePosition(product.getStoragePosition()); // 補上這行
            existingProduct.setProductDescription(product.getProductDescription());
            existingProduct.setProductStatus(product.getProductStatus());

            // 處理圖片
            if (file == null || file.isEmpty()) {
                existingProduct.setProductImage(oldImage);
            } else {
                handleMultiImageUpload(existingProduct, file, extraFiles);
            }

            productService.updateProduct(existingProduct);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("fail: " + e.getMessage());
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
        // 【新增】點擊率累加邏輯
        productService.incrementClickCount(id);

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
        return fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");}
    //===== 快速更新庫存 (用於列表內嵌編輯) ===========================================================================
    @PutMapping("/update-stock/{id}")
    public ResponseEntity<?> updateStock(@PathVariable Integer id, @RequestParam("stock") Integer stock) {
        try {
            Product product = productService.getProductById(id);
            if (product != null) {
                // 確保分類資訊被載入，避免更新時遺失關聯 (強制觸發 Lazy Load)
                if (product.getCategories() != null) {
                    product.getCategories().size(); 
                }
                
                product.setProductStock(stock);
                productService.updateProduct(product);
                return ResponseEntity.ok("success");
            }
            return ResponseEntity.status(404).body("Product not found");
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}