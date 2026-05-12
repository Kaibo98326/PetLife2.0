package com.petlife.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.petlife.model.Category;
import com.petlife.model.Discount;
import com.petlife.model.Product;
import com.petlife.repository.ProductResponseDTO;
import com.petlife.service.CategoryService;
import com.petlife.service.ProductService;


//匯入活動邏輯
import com.petlife.service.DiscountService;

@RestController 
@RequestMapping("/api/products") 
public class InnerProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;
//活動物件
    @Autowired
    private DiscountService discountService;
    
    
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
            if (p.getCategories() != null && !p.getCategories().isEmpty()) {
                String names = p.getCategories().stream()
                        .map(Category::getCategoryName)
                        .collect(java.util.stream.Collectors.joining(", "));
                p.setCategoryName(names);
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
            e.printStackTrace();
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
//下方有同樣功能的取得單一商品，包含活動標籤，我先把這個方法隱藏
    /*
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
*/
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
    
    //取單一商品也包含取得活動標籤
  //===== (整合活動標籤) ============
    @GetMapping("/detail/{id}")
    public ResponseEntity<ProductResponseDTO> getProductDetail(@PathVariable Integer id) {
        // 1. 從資料庫撈出原始商品資料 (修正：改用 productService，解決 productRepository 找不到的問題)
        Product product = productService.getProductById(id);
        if (product == null) return ResponseEntity.notFound().build();

        // 用來安全取得分類資訊，解決 getCategory 報錯
        Integer firstCategoryId = null;
        String categoryNames = "";
        if (product.getCategories() != null && !product.getCategories().isEmpty()) {
            firstCategoryId = product.getCategories().iterator().next().getCategoryId();
            categoryNames = product.getCategories().stream()
                    .map(Category::getCategoryName)
                    .collect(java.util.stream.Collectors.joining(", "));
        }

        // 3. 建立要傳給前端的 DTO
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setProductId(product.getProductId());
        dto.setProductName(product.getProductName());
        dto.setProductPrice(product.getProductPrice());
        dto.setProductStock(product.getProductStock());
        dto.setProductImage(product.getProductImage());
        
        // 填入剛才抓出來的安全分類資料
        dto.setCategoryId(firstCategoryId);
        dto.setCategoryName(categoryNames);

        // 4. 呼叫 DiscountService 查水表
        if (firstCategoryId != null) {
            Discount activeDiscount = discountService.findBestActiveDiscountForProduct(
                product.getProductId(), 
                firstCategoryId
            );

            // 5. 如果有找到活動，就貼上標籤
            if (activeDiscount != null) {
                dto.setHasActiveDiscount(true);
                dto.setActiveDiscountName(activeDiscount.getDiscountName());
                dto.setDiscountType(activeDiscount.getDiscountType().getDiscountTypeId().toString());
            }
        }

        return ResponseEntity.ok(dto);
    }
}