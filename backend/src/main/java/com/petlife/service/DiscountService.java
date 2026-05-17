package com.petlife.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petlife.model.Category;
import com.petlife.model.Discount;
import com.petlife.model.DiscountCategory;
import com.petlife.model.DiscountProduct;
import com.petlife.model.DiscountType;
import com.petlife.model.Product;
import com.petlife.repository.CategoryRepository;
import com.petlife.repository.DiscountCategoryRepository;
import com.petlife.repository.DiscountProductRepository;
import com.petlife.repository.DiscountRepository;
import com.petlife.repository.DiscountTypeRepository;
import com.petlife.repository.ProductRepository;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
@Service
@Transactional // 確保事務完整性，失敗會自動回滾
public class DiscountService {

    @Autowired
    private DiscountRepository discountRepository;
    @Autowired
    private DiscountCategoryRepository discountCategoryRepository;
    @Autowired
    private DiscountProductRepository discountProductRepository;
    @Autowired
    private DiscountTypeRepository discountTypeRepository;
    
    //這兩個 Repository，用來向資料庫要真實的商品與分類
    @Autowired
    private CategoryRepository categoryRepository; 
    @Autowired
    private ProductRepository productRepository;   

    public List<DiscountType> getAllDiscountTypes() {
        return discountTypeRepository.findAll();
    }

 // ✨ 修改：還原為原來的 List<Discount> 全量查詢結構，確保前端本地進階篩選功能完好如初
    public List<Discount> getAllDiscounts() {
        return discountRepository.findAll();
    }
    

    //：加入 addonCategoryIds 參數，並區分 Main 與 Addon
 // ✨ 修改：加入 tagCategoryId 參數，用來接收前端選取的標籤 ID
    public void saveDiscountWithDetails(Discount discount, 
                                        List<Integer> categoryIds, 
                                        List<Integer> mainProductIds, 
                                        List<Integer> addonProductIds,
                                        List<Integer> addonCategoryIds,
                                        Integer tagCategoryId) { // ✨ 新增：接收前端傳來的標籤 ID
        
        Discount savedDiscount = discountRepository.save(discount);

        
            
        	// 處理主項目 (Main) - 嚴格依賴 scopeType
            if (discount.getScopeType() == 1) {
                if (categoryIds != null) {
                    for (Integer catId : categoryIds) {
                        Category category = categoryRepository.findById(catId).orElse(null);
                        if (category != null) {
                            DiscountCategory dc = new DiscountCategory(savedDiscount, category, "Main");
                            discountCategoryRepository.save(dc);
                        }
                    }
                }
            } else if (discount.getScopeType() == 2) {
                if (mainProductIds != null) {
                    for (Integer prodId : mainProductIds) {
                        Product product = productRepository.findById(prodId).orElse(null);
                        if (product != null) {
                            DiscountProduct dp = new DiscountProduct(savedDiscount, product, "Main");
                            discountProductRepository.save(dp);
                        }
                    }
                }
            }

            // ✨ 修改：處理副項目 (Addon) - 完全脫離 scopeType 限制！只要前端有傳，就獨立存入對應關聯表
            if (addonCategoryIds != null) {
                for (Integer catId : addonCategoryIds) {
                    Category category = categoryRepository.findById(catId).orElse(null);
                    if (category != null) {
                        DiscountCategory dc = new DiscountCategory(savedDiscount, category, "Addon");
                        discountCategoryRepository.save(dc);
                    }
                }
            }
            if (addonProductIds != null) {
                for (Integer prodId : addonProductIds) {
                    Product product = productRepository.findById(prodId).orElse(null);
                    if (product != null) {
                        DiscountProduct dp = new DiscountProduct(savedDiscount, product, "Addon");
                        discountProductRepository.save(dp);
                    }
                }
            }

           
        // ✨ 新增：處理活動標籤 (Tag) - 因為不管 scopeType 是 1 還是 2，都有可能掛載標籤，所以寫在 if-else 外面
        if (tagCategoryId != null) {
            Category tagCategory = categoryRepository.findById(tagCategoryId).orElse(null);
            if (tagCategory != null) {
                // 將這個分類的角色設定為 "Tag"
                DiscountCategory dc = new DiscountCategory(savedDiscount, tagCategory, "Tag");
                discountCategoryRepository.save(dc);
            }
        
    

        }
    }
   //刪除
    public void deleteDiscount(Integer id) {
        discountRepository.deleteById(id);
    }
    
    /**
     * 動態尋找該商品符合的最佳活動
     * 支援「單品優先於分類」以及「時間區間自動過濾」
     */
    public Discount findBestActiveDiscountForProduct(Integer productId, Integer categoryId) {
        
    	// 1. 取得現在日期 (改用 Java 8 新版的 LocalDate)
        java.time.LocalDate today = java.time.LocalDate.now();

        // 2. 撈出所有活動
        List<Discount> allDiscounts = discountRepository.findAll();

        // 3. 過濾出「進行中」的活動列表
        List<Discount> activeDiscounts = allDiscounts.stream()
            .filter(d -> "active".equals(d.getStatus()))
            .filter(d -> d.getStartDate() != null && d.getEndDate() != null)
            // ✨ 修正：改用 isBefore() 和 isAfter() 來比較 LocalDate
            // !today.isBefore(startDate) 代表「今天 >= 開始日」
            // !today.isAfter(endDate) 代表「今天 <= 結束日」
            .filter(d -> !today.isBefore(d.getStartDate()) && !today.isAfter(d.getEndDate()))
            .collect(Collectors.toList());

        // --- 第一優先權：檢查「指定單品 (Scope 2)」 ---
        for (Discount d : activeDiscounts) {
            if (d.getScopeType() == 2) {
                // 檢查該活動的商品關聯清單中是否包含此商品，且身分為 Main
                boolean isMatch = d.getDiscountProducts().stream()
                    .anyMatch(dp -> dp.getProduct().getProductId().equals(productId) 
                                 && "Main".equals(dp.getProductRole()));
                if (isMatch) return d;
            }
        }

        // --- 第二優先權：檢查「指定分類 (Scope 1)」 ---
        for (Discount d : activeDiscounts) {
            if (d.getScopeType() == 1) {
                // 檢查該活動的分類關聯清單中是否包含此分類，且身分為 Main
                boolean isMatch = d.getDiscountCategories().stream()
                    .anyMatch(dc -> dc.getCategory().getCategoryId().equals(categoryId) 
                                 && "Main".equals(dc.getCategoryRole()));
                if (isMatch) return d;
            }
        }

        // 4. 若皆無符合活動，回傳 null
        return null;
    }
    
    
    
    
}