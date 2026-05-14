package com.petlife.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petlife.model.Product;
import com.petlife.repository.ProductRepository;

@Service
@Transactional 
public class ProductService {

	@Autowired
	private com.petlife.repository.DiscountRepository discountRepository;
	
    private final ProductRepository productRepository;
    private final com.petlife.repository.CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, com.petlife.repository.CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
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
        if (product.getCategoryIds() != null) {
            java.util.List<Integer> validIds = product.getCategoryIds().stream()
                    .filter(id -> id != null)
                    .collect(java.util.stream.Collectors.toList());
            if (!validIds.isEmpty()) {
                List<com.petlife.model.Category> cats = categoryRepository.findAllById(validIds);
                product.setCategories(cats);
            } else {
                product.setCategories(new java.util.ArrayList<>());
            }
        } else {
            // 如果沒傳 categoryIds，保留原本的關聯 (需從資料庫先查出原本的關聯)
            Product existing = productRepository.findById(product.getProductId()).orElse(null);
            if (existing != null) {
                product.setCategories(existing.getCategories());
            }
        }
        return productRepository.save(product);
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
        
        for (com.petlife.model.Discount d : activeDiscounts) {
            String badgeName = d.getDiscountName(); // 要顯示在前端的徽章文字 (活動名稱)
            
            if (d.getScopeType() == 1) { // 指定分類
                List<Integer> catIds = d.getDiscountCategories().stream()
                    .filter(dc -> "Main".equals(dc.getCategoryRole()))
                    .map(dc -> dc.getCategory().getCategoryId())
                    .collect(Collectors.toList());
                
                if (!catIds.isEmpty()) {
                    List<Integer> pIds = productRepository.findProductIdsByCategoryIds(catIds);
                    for (Integer pid : pIds) {
                        validProductIds.add(pid);
                        productBadgeMap.put(pid, badgeName); // 記錄徽章
                    }
                }
            } else if (d.getScopeType() == 2) { // 指定單品
                List<Integer> pIds = d.getDiscountProducts().stream()
                    .filter(dp -> "Main".equals(dp.getProductRole()))
                    .map(dp -> dp.getProduct().getProductId())
                    .collect(Collectors.toList());
                
                for (Integer pid : pIds) {
                    validProductIds.add(pid);
                    productBadgeMap.put(pid, badgeName); // 記錄徽章
                }
            }
        }
        
        // 如果這個標籤下的活動剛好都沒圈選到商品，直接回傳空
        if (validProductIds.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // 3. 透過 ID 清單查詢商品並分頁
        Page<Product> productPage = productRepository.findByProductIdIn(new java.util.ArrayList<>(validProductIds), pageable);
        
        // 4. 將活動名稱塞入虛擬欄位 activityBadge，讓 Vue 顯示
        for (Product p : productPage.getContent()) {
            p.setActivityBadge(productBadgeMap.get(p.getProductId()));
        }
        
        return productPage;
    }
    
}