package com.petlife.service;

import java.util.List;

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
    
}