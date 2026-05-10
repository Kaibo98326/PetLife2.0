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
import com.petlife.repository.DiscountCategoryRepository;
import com.petlife.repository.DiscountProductRepository;
import com.petlife.repository.DiscountRepository;
import com.petlife.repository.DiscountTypeRepository;

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
    private DiscountTypeRepository discountTypeRepository; // ✨ 新增這行

    // ✨ 新增這個方法：取得所有折扣類型供前端選單使用
    public List<DiscountType> getAllDiscountTypes() {
        return discountTypeRepository.findAll();
    }

    //  查詢：取得所有活動供前端表格顯示
    public List<Discount> getAllDiscounts() {
        return discountRepository.findAll();
    }

    // 儲存：根據活動範圍 (scope_type) 決定存入哪張關聯表
 // 儲存：根據活動範圍 (scope_type) 決定存入哪張關聯表，並區分 Main 與 Addon 角色
    public void saveDiscountWithDetails(Discount discount, 
                                        List<Integer> categoryIds, 
                                        List<Integer> mainProductIds, 
                                        List<Integer> addonProductIds) {
        
        // 1. 先存入活動主表，取得帶有 ID 的 savedDiscount
        Discount savedDiscount = discountRepository.save(discount);

        // 2. 判斷範圍：1 = 分類級, 2 = 單品級
        if (discount.getScopeType() == 1 && categoryIds != null) {
            
            for (Integer catId : categoryIds) {
                Category category = new Category();       // 使用無參數建構子
                category.setCategoryId(catId);            // 手動設定 ID
                DiscountCategory dc = new DiscountCategory(savedDiscount, category);
                discountCategoryRepository.save(dc);
            }
            
        } else if (discount.getScopeType() == 2) {
            
            // 處理主商品 (Main)
            if (mainProductIds != null) {
                for (Integer prodId : mainProductIds) {
                    Product product = new Product();      // 使用無參數建構子
                    product.setProductId(prodId);         // 手動設定 ID
                    // 寫入關聯表，明確賦予 "Main" 角色
                    DiscountProduct dp = new DiscountProduct(savedDiscount, product, "Main");
                    discountProductRepository.save(dp);
                }
            }
            
            // 處理加購/贈品 (Addon)
            if (addonProductIds != null) {
                for (Integer prodId : addonProductIds) {
                    Product product = new Product();      // 使用無參數建構子
                    product.setProductId(prodId);         // 手動設定 ID
                    // 寫入關聯表，明確賦予 "Addon" 角色
                    DiscountProduct dp = new DiscountProduct(savedDiscount, product, "Addon");
                    discountProductRepository.save(dp);
                }
            }
        }
    }
    //  刪除：刪除活動
    public void deleteDiscount(Integer id) {
        discountRepository.deleteById(id);
    }
}