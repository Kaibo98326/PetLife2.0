package com.petlife.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petlife.model.Product;
import com.petlife.model.Category;
import com.petlife.repository.ProductRepository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional 
public class ProductService {

	@Autowired
	private com.petlife.repository.DiscountRepository discountRepository;
	
    private final ProductRepository productRepository;
    private final com.petlife.repository.CategoryRepository categoryRepository;
    private final com.petlife.repository.ProductImageRepository productImageRepository;

    public ProductService(
            ProductRepository productRepository, 
            com.petlife.repository.CategoryRepository categoryRepository,
            com.petlife.repository.ProductImageRepository productImageRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
    }

//===== 查全部商品 (不分頁) ====================================================================================
    @Transactional(readOnly = true) // 查詢操作設定為唯讀，優化效能
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

//===== 依 ID 查商品 ========================================================================================
    @Transactional(readOnly = true) // 查詢操作設定為唯讀，優化效能
    public Product getProductById(Integer id) {
        return productRepository.findById(id).orElse(null);
    }

//===== 新增&更新商品 ========================================================================================
    public Product addProduct(Product product) {
        // 自動下架邏輯：庫存為 0 時自動設為下架 (0)
        if (product.getProductStock() != null && product.getProductStock() <= 0) {
            product.setProductStatus(0);
        }
        
        if (product.getCategoryIds() != null) {
            java.util.List<Integer> validIds = product.getCategoryIds().stream()
                    .filter(id -> id != null)
                    .collect(java.util.stream.Collectors.toList());
            if (!validIds.isEmpty()) {
                List<com.petlife.model.Category> cats = categoryRepository.findAllById(validIds);
                product.setCategories(cats);
            }
        }
        return productRepository.save(product);
    }

    public Product updateProduct(Product product) {
        if (product == null || product.getProductId() == null) {
            return null;
        }

        Product existing = productRepository.findById(product.getProductId()).orElse(null);
        if (existing == null) return null;

        // 1. 基本欄位更新
        existing.setProductName(product.getProductName());
        existing.setProductPrice(product.getProductPrice());
        existing.setProductStock(product.getProductStock());
        existing.setLowStock(product.getLowStock());
        existing.setStoragePosition(product.getStoragePosition());
        existing.setProductDescription(product.getProductDescription());
        existing.setProductStatus(product.getProductStatus());

        // 自動下架邏輯：如果更新後庫存為 0，強制設為下架 (0)
        if (existing.getProductStock() != null && existing.getProductStock() <= 0) {
            existing.setProductStatus(0);
        }
        
        // 只有在有傳入新圖片時才更新圖片路徑
        if (product.getProductImage() != null && !product.getProductImage().isEmpty()) {
            existing.setProductImage(product.getProductImage());
        }

        // 2. 處理分類關聯
        // 修正：只有在明確傳入 categoryIds 時才更新，避免被預設的空 List 誤導清空
        if (product.getCategoryIds() != null && !product.getCategoryIds().isEmpty()) {
            java.util.List<Integer> validIds = product.getCategoryIds().stream()
                    .filter(id -> id != null)
                    .collect(java.util.stream.Collectors.toList());
            if (!validIds.isEmpty()) {
                List<com.petlife.model.Category> cats = categoryRepository.findAllById(validIds);
                existing.setCategories(cats);
            }
        }

        // 3. 處理多圖關聯
        // 修正：僅添加「新」圖片 (id 為 null 的)，避免重複添加已存在的圖片導致 500 錯誤
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            for (com.petlife.model.ProductImage img : product.getImages()) {
                if (img.getId() == null) {
                    img.setProduct(existing);
                    existing.getImages().add(img);
                }
            }
        }

        return productRepository.save(existing);
    }

//===== 刪除商品 ============================================================================================
    public void deleteProduct(Integer id) {
        productRepository.deleteById(id);
    }

//===== 依分類查商品 (分頁) ===================================================================================
    @Transactional(readOnly = true) // 查詢操作設定為唯讀，優化效能
    public Page<Product> getProductsByCategory(Integer categoryId, int page, int size) {
        // 為了不影響後台，這裡預設加上最新上架排序
        Pageable pageable = PageRequest.of(page - 1, size, org.springframework.data.domain.Sort.by("productId").descending());
        return productRepository.findByCategory(categoryId, pageable);
    }

    @Transactional(readOnly = true) // 查詢操作設定為唯讀，優化效能
    public Page<Product> searchProducts(String keyword, int page, int size) {
        // 為了不影響後台，這裡預設加上最新上架排序
        Pageable pageable = PageRequest.of(page - 1, size, org.springframework.data.domain.Sort.by("productId").descending());
        return productRepository.searchByName(keyword, pageable);
    }
    
    // 【新增】支援動態分頁與排序的版本
    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        return productRepository.searchByName(keyword, pageable);
    }
    
    // 【新增】支援分類動態分頁與排序的版本
    @Transactional(readOnly = true)
    public Page<Product> getProductsByCategory(Integer categoryId, Pageable pageable) {
        return productRepository.findByCategory(categoryId, pageable);
    }
    
    // 【商城前台專用】僅查詢上架商品
    @Transactional(readOnly = true)
    public Page<Product> searchActiveProducts(String keyword, Pageable pageable) {
        return productRepository.searchActiveByName(keyword, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<Product> getActiveProductsByCategory(Integer categoryId, Pageable pageable) {
        return productRepository.findActiveByCategory(categoryId, pageable);
    }
    
    
//===== 獲取低庫存商品清單 =========================================================================

    @Transactional(readOnly = true)
    public Page<Product> getLowStockProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return productRepository.findLowStock(pageable);
    }

//===== 獲取低庫存商品總數 ===============================================================================

    @Transactional(readOnly = true)
    public long getLowStockCount() {
        return productRepository.countLowStock();
    }    
    
//===== 後台商品 批次上下架處理 ======================================================================================
    
    public void batchUpdateStatus(List<Integer> ids, Integer status) {
        productRepository.batchUpdateStatus(ids, status);
    }

    //===== 複合式篩選 (Specification) =========================================================================
    @Transactional(readOnly = true)
    public Page<Product> getCompositeProducts(
            String keyword, 
            Integer categoryId, 
            Integer status, 
            Double minPrice, 
            Double maxPrice, 
            Integer minStock, 
            Integer maxStock, 
            Boolean lowStock,
            int page, 
            int size) {
        
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                predicates.add(cb.like(root.get("productName"), "%" + keyword + "%"));
            }

            if (categoryId != null && categoryId != 0) {
                Join<Product, Category> categoryJoin = root.join("categories");
                predicates.add(cb.or(
                    cb.equal(categoryJoin.get("categoryId"), categoryId),
                    cb.equal(categoryJoin.get("parentId"), categoryId)
                ));
                query.distinct(true);
            }

            if (status != null && status != -1) {
                predicates.add(cb.equal(root.get("productStatus"), status));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("productPrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("productPrice"), maxPrice));
            }

            if (minStock != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("productStock"), minStock));
            }
            if (maxStock != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("productStock"), maxStock));
            }

            if (lowStock != null && lowStock) {
                predicates.add(cb.lessThanOrEqualTo(
                    root.get("productStock"), 
                    cb.coalesce(root.get("lowStock"), 10)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page - 1, size, org.springframework.data.domain.Sort.by("productId").descending());
        return productRepository.findAll(spec, pageable);
    }

    // ===== 熱門排行相關 =====
    @Transactional(readOnly = true)
    public List<Product> getTop5HotProducts() {
        return productRepository.findTop10ByClickCount(PageRequest.of(0, 5));
    }

    public void incrementClickCount(Integer productId) {
        productRepository.incrementClickCount(productId);
    }
    
    
 //  新增：透過活動標籤(Type 3)獲取適用的商品，並賦予活動徽章
    @Transactional(readOnly = true)
    public Page<Product> getProductsByActivityTag(Integer tagId, Pageable pageable) {
        
        // 1. 找出該標籤綁定的「進行中」活動
        List<com.petlife.model.Discount> activeDiscounts = discountRepository.findActiveDiscountsByTagId(tagId, java.time.LocalDate.now());
        
        if (activeDiscounts.isEmpty()) {
            return Page.empty(pageable); // 標籤下沒有進行中的活動，回傳空分頁
        }
        
        // 2. 收集所有符合條件的商品 ID (使用 Set 避免重複)
        java.util.Set<Integer> validProductIds = new java.util.HashSet<>();
        java.util.Map<Integer, String> productBadgeMap = new java.util.HashMap<>(); // 紀錄商品對應的活動名稱
        java.util.Map<Integer, String> productRoleMap = new java.util.HashMap<>(); // 紀錄商品的角色 (Main 還是 Addon)
        // ✨ 新增：紀錄商品對應的活動類型 ID (1, 2, 3, 4, 5)
        java.util.Map<Integer, String> productTypeMap = new java.util.HashMap<>(); 
        
        for (com.petlife.model.Discount d : activeDiscounts) {
            String badgeName = d.getDiscountName(); // 要顯示在前端的徽章文字
            // ✨ 新增：獲取活動類型 ID
            String typeIdStr = d.getDiscountType() != null ? d.getDiscountType().getDiscountTypeId().toString() : "";
            
            // ✨ 修改：同時抓取 Main 與 Addon 的分類關聯商品 (修正為 Set 避免 Type mismatch 錯誤)
            java.util.Set<com.petlife.model.DiscountCategory> dCats = d.getDiscountCategories();
            for(com.petlife.model.DiscountCategory dc : dCats) {
                if (dc.getCategory() == null) continue; // ✨ 新增防呆：防範分類關聯為空
                if("Main".equals(dc.getCategoryRole()) || "Addon".equals(dc.getCategoryRole())) {
                    List<Integer> pIds = productRepository.findProductIdsByCategoryIds(java.util.Collections.singletonList(dc.getCategory().getCategoryId()));
                    for (Integer pid : pIds) {
                        validProductIds.add(pid);
                        productBadgeMap.put(pid, badgeName); 
                        productRoleMap.put(pid, dc.getCategoryRole()); // 標記角色
                        productTypeMap.put(pid, typeIdStr); // ✨ 新增：綁定活動類型
                    }
                }
            }
            
            // ✨ 修改：同時抓取 Main 與 Addon 的單品關聯商品 (修正為 Set 避免 Type mismatch 錯誤)
            java.util.Set<com.petlife.model.DiscountProduct> dProds = d.getDiscountProducts();
            for(com.petlife.model.DiscountProduct dp : dProds) {
                if (dp.getProduct() == null) continue; // ✨ 新增防呆：防範商品關聯為空
                if("Main".equals(dp.getProductRole()) || "Addon".equals(dp.getProductRole())) {
                    Integer pid = dp.getProduct().getProductId();
                    validProductIds.add(pid);
                    productBadgeMap.put(pid, badgeName);
                    productRoleMap.put(pid, dp.getProductRole()); // 標記角色
                    productTypeMap.put(pid, typeIdStr); // ✨ 新增：綁定活動類型
                }
            }
        }
        
        // 如果這個標籤下的活動剛好都沒圈選到商品，直接回傳空
        if (validProductIds.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // 3. 透過 ID 清單查詢商品並分頁
        Page<Product> productPage = productRepository.findByProductIdIn(new java.util.ArrayList<>(validProductIds), pageable);
        
        // 4. 將活動名稱、角色與活動類型塞入虛擬欄位，讓 Vue 顯示
        for (Product p : productPage.getContent()) {
            p.setActivityBadge(productBadgeMap.get(p.getProductId()));
            p.setProductRole(productRoleMap.get(p.getProductId())); 
            // ✨ 新增：將活動類型 ID 寫入商品虛擬欄位
            p.setDiscountType(productTypeMap.get(p.getProductId())); 
        }
        
        return productPage;
    }
    
}