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
    
    // ✨ 引入這兩個 Repository，用來向資料庫要真實的商品與分類
    @Autowired
    private CategoryRepository categoryRepository; 
    @Autowired
    private ProductRepository productRepository;   

    public List<DiscountType> getAllDiscountTypes() {
        return discountTypeRepository.findAll();
    }

    public List<Discount> getAllDiscounts() {
        return discountRepository.findAll();
    }

    // ✨ 修正：加入 addonCategoryIds 參數，並區分 Main 與 Addon
    public void saveDiscountWithDetails(Discount discount, 
                                        List<Integer> categoryIds, 
                                        List<Integer> mainProductIds, 
                                        List<Integer> addonProductIds,
                                        List<Integer> addonCategoryIds) { // 👈 接收前端傳來的新參數
        
        Discount savedDiscount = discountRepository.save(discount);

        if (discount.getScopeType() == 1) {
            
            // 處理主分類 (Main)
            if (categoryIds != null) {
                for (Integer catId : categoryIds) {
                    Category category = categoryRepository.findById(catId).orElse(null);
                    if (category != null) {
                        DiscountCategory dc = new DiscountCategory(savedDiscount, category, "Main");
                        discountCategoryRepository.save(dc);
                    }
                }
            }
            
            // 處理副分類 (Addon) - 用於分類的買 N 送 M 或加購
            if (addonCategoryIds != null) {
                for (Integer catId : addonCategoryIds) {
                    Category category = categoryRepository.findById(catId).orElse(null);
                    if (category != null) {
                        DiscountCategory dc = new DiscountCategory(savedDiscount, category, "Addon");
                        discountCategoryRepository.save(dc);
                    }
                }
            }
            
        } else if (discount.getScopeType() == 2) {
            
            // 處理主商品 (Main)
            if (mainProductIds != null) {
                for (Integer prodId : mainProductIds) {
                    Product product = productRepository.findById(prodId).orElse(null);
                    if (product != null) {
                        DiscountProduct dp = new DiscountProduct(savedDiscount, product, "Main");
                        discountProductRepository.save(dp);
                    }
                }
            }
            
            // 處理副商品 (Addon)
            if (addonProductIds != null) {
                for (Integer prodId : addonProductIds) {
                    Product product = productRepository.findById(prodId).orElse(null);
                    if (product != null) {
                        DiscountProduct dp = new DiscountProduct(savedDiscount, product, "Addon");
                        discountProductRepository.save(dp);
                    }
                }
            }
        }
    }

    public void deleteDiscount(Integer id) {
        discountRepository.deleteById(id);
    }
}