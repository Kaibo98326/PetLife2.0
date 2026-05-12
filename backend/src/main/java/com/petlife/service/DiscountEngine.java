package com.petlife.service;


import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.petlife.model.Discount;
import com.petlife.repository.CartItemDTO; 

@Service
public class DiscountEngine {

    @Autowired
    private DiscountCalculationService calculationService;

    public BigDecimal executeDiscount(List<CartItemDTO> cartItems, List<Discount> allActiveDiscounts) {
        BigDecimal totalSaved = BigDecimal.ZERO;

        // 1. 依照優先權分組 (核心防線 1：單品 > 分類)
        List<Discount> productLevelDiscounts = allActiveDiscounts.stream()
                .filter(d -> d.getScopeType() == 2).collect(Collectors.toList());

        List<Discount> categoryLevelDiscounts = allActiveDiscounts.stream()
                .filter(d -> d.getScopeType() == 1).collect(Collectors.toList());

        // ==============================================================
        // ✨ 補充邏輯 7：同層級最優解 (Intra-Level Best Offer)
        // ==============================================================
        
        // 【單品層級】
        // 1-1. 先進行「假算(Dry Run)」測試，看每個活動能幫客人省多少錢
        // 然後依據省下的金額，由高到低 (降冪) 排序
        productLevelDiscounts.sort((d1, d2) -> {
            BigDecimal saved1 = dispatchCalculation(cartItems, d1, true); // true = 假算
            BigDecimal saved2 = dispatchCalculation(cartItems, d2, true);
            return saved2.compareTo(saved1); // 大的排前面
        });

        // 1-2. 排序完成後，再依序「真算」，確保最高金額的活動優先吃掉商品！
        for (Discount discount : productLevelDiscounts) {
            BigDecimal saved = dispatchCalculation(cartItems, discount, false); // false = 真算並綁定
            totalSaved = totalSaved.add(saved);
        }

        // 【分類層級】(與上方邏輯完全相同，確保分類活動也是擇優套用)
        categoryLevelDiscounts.sort((d1, d2) -> {
            BigDecimal saved1 = dispatchCalculation(cartItems, d1, true); 
            BigDecimal saved2 = dispatchCalculation(cartItems, d2, true);
            return saved2.compareTo(saved1); 
        });

        for (Discount discount : categoryLevelDiscounts) {
            BigDecimal saved = dispatchCalculation(cartItems, discount, false); 
            totalSaved = totalSaved.add(saved);
        }

        // ==============================================================
        // ✨ 補充邏輯 8：最低結帳金額保護 (Minimum Checkout Protection)
        // ==============================================================
        
        // 算出購物車裡原本的總價
        BigDecimal cartTotalAmount = BigDecimal.ZERO;
        for(CartItemDTO item : cartItems) {
            cartTotalAmount = cartTotalAmount.add(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }
        
        // 設定老闆的底線：整筆訂單就算再怎麼疊加折抵，客人最少必須付 1 元！
        BigDecimal minPayable = BigDecimal.ONE; 
        // 算出系統「最多允許折掉多少錢」
        BigDecimal maxAllowedDiscount = cartTotalAmount.subtract(minPayable);

        // 如果總折扣不小心超過了商品總價 (比如 500 元折了 600 元)
        if (totalSaved.compareTo(maxAllowedDiscount) > 0) {
            // 強制將折扣縮水到老闆的底線，保護毛利不被擊穿！
            totalSaved = maxAllowedDiscount;
        }

        // 最終防呆：避免總折扣變成負數
        return totalSaved.compareTo(BigDecimal.ZERO) > 0 ? totalSaved : BigDecimal.ZERO;
    }

    /**
     * 邏輯分發中心
     * ✨ 新增 isDryRun 參數傳遞給計算服務
     */
    private BigDecimal dispatchCalculation(List<CartItemDTO> cartItems, Discount discount, boolean isDryRun) {
        List<CartItemDTO> eligibleItems = filterEligibleItems(cartItems, discount, "Main");
        if (eligibleItems.isEmpty()) return BigDecimal.ZERO;

        Integer typeId = discount.getDiscountType().getDiscountTypeId();
        
        switch (typeId) {
            case 1: return calculationService.calculatePercentageDiscount(eligibleItems, discount, isDryRun);
            case 2: return calculationService.calculateFixedDiscount(eligibleItems, discount, isDryRun);
            case 3: { 
                List<CartItemDTO> freeItems = filterEligibleItems(cartItems, discount, "Addon");
                return calculationService.calculateBuyNGetMDiscount(eligibleItems, freeItems, discount, isDryRun);
            }
            case 4: { 
                List<CartItemDTO> addonItems = filterEligibleItems(cartItems, discount, "Addon");
                return calculationService.calculateAddOnDiscount(eligibleItems, addonItems, discount, isDryRun);
            }
            case 5: return calculationService.calculateBundleDiscount(eligibleItems, discount, isDryRun);
            default: return BigDecimal.ZERO;
        }
    }

    // ... 下方的 filterEligibleItems 保持原樣不變 ...
    private List<CartItemDTO> filterEligibleItems(List<CartItemDTO> cartItems, Discount discount, String role) { 
        // 原本的程式碼...
        return cartItems.stream().filter(item -> !item.isProcessed()).collect(Collectors.toList()); 
    }
}
