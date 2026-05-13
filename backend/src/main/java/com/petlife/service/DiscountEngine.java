package com.petlife.service;


import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.petlife.model.Discount;
import com.petlife.repository.CartItemDTO; 
//決定誰有資格參加活動、誰先執行、哪一個活動最划算 ---->大腦
@Service
public class DiscountEngine {

	// 讓 Spring 幫我們注入剛剛寫好的會計部 (DiscountCalculationService)
    @Autowired
    private DiscountCalculationService calculationService;

    // 執行整筆訂單折扣計算的核心大門 (Controller 會呼叫這裡)
    public BigDecimal executeDiscount(List<CartItemDTO> cartItems, List<Discount> allActiveDiscounts) {
        // 準備一個變數，用來累加這筆訂單「總共幫客人省下多少錢」
        BigDecimal totalSaved = BigDecimal.ZERO;

        // 【核心邏輯 1：執行優先權 - 先分組】把所有活動依照 ScopeType 拆分成單品與分類兩大陣營
        // 撈出所有「單品層級」的活動
        List<Discount> productLevelDiscounts = allActiveDiscounts.stream()
                .filter(d -> d.getScopeType() == 2).collect(Collectors.toList());

        // 撈出所有「分類層級」的活動
        List<Discount> categoryLevelDiscounts = allActiveDiscounts.stream()
                .filter(d -> d.getScopeType() == 1).collect(Collectors.toList());

        // ==============================================================
        // ✨ 補充邏輯 7：同層級最優解 (Intra-Level Best Offer)
        // ==============================================================
        
        // 【第一梯次：處理單品層級】
        // 1-1. 在真正打折之前，先對單品活動清單進行「假算」並排序
        productLevelDiscounts.sort((d1, d2) -> {
            // 呼叫分發中心，最後一個參數帶入 true，代表開啟「假算(Dry Run)」模式
            BigDecimal saved1 = dispatchCalculation(cartItems, d1, true); 
            // 同樣對第二個活動假算
            BigDecimal saved2 = dispatchCalculation(cartItems, d2, true);
            // 比較兩者省下的金額，降冪排序（省最多的活動會被排在陣列的最前面）
            return saved2.compareTo(saved1); 
        });

        // 1-2. 排序完成後，最猛的活動已經排在最前面了，開始跑迴圈「真算」
        for (Discount discount : productLevelDiscounts) {
            // 呼叫分發中心，最後一個參數帶 false，代表「真算且綁定標籤」
            BigDecimal saved = dispatchCalculation(cartItems, discount, false); 
            // 將算出來的錢，累加進總折扣金裡
            totalSaved = totalSaved.add(saved);
        }

        // 【第二梯次：處理分類層級】(邏輯同上，因為經過上一步，部分商品已被互斥標記，所以分類只會對剩下的商品發生作用)
        // 2-1. 對分類活動進行假算排序
        categoryLevelDiscounts.sort((d1, d2) -> {
            BigDecimal saved1 = dispatchCalculation(cartItems, d1, true); 
            BigDecimal saved2 = dispatchCalculation(cartItems, d2, true);
            // 省最多的分類活動排前面
            return saved2.compareTo(saved1); 
        });

        // 2-2. 依序真算並綁定
        for (Discount discount : categoryLevelDiscounts) {
            BigDecimal saved = dispatchCalculation(cartItems, discount, false); 
            // 累加分類活動省下的錢
            totalSaved = totalSaved.add(saved);
        }

        // ==============================================================
        // ✨ 補充邏輯 8：最低結帳金額保護 (Minimum Checkout Protection)
        // ==============================================================
        
        // 準備算出這筆訂單如果不打折，原本總共要多少錢
        BigDecimal cartTotalAmount = BigDecimal.ZERO;
        // 跑迴圈累加購物車商品總價
        for(CartItemDTO item : cartItems) {
            cartTotalAmount = cartTotalAmount.add(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }
        
        // 設定公司的底線：整筆訂單就算再怎麼疊加折扣，客人在結帳時「最少必須付 1 元」
        BigDecimal minPayable = BigDecimal.ONE; 
        // 算出系統「最多允許折掉多少錢」= (原本總價 - 老闆底線的 1 元)
        BigDecimal maxAllowedDiscount = cartTotalAmount.subtract(minPayable);

        // 如果剛剛所有活動算出來的總折扣，不小心超過了系統允許的極限
        if (totalSaved.compareTo(maxAllowedDiscount) > 0) {
            // 強制將總折扣縮水，鎖死在最大允許範圍內，防止毛利被擊穿！
            totalSaved = maxAllowedDiscount;
        }

        // 最終防呆：避免總折扣不知為何變成負數，若是負數一律改回 0 元
        return totalSaved.compareTo(BigDecimal.ZERO) > 0 ? totalSaved : BigDecimal.ZERO;
    }

    /**
     * 邏輯分發中心：判斷活動類型並呼叫對應算法
     * ✨ 升級：多接收了一個 isDryRun 參數，負責往計算中心傳遞
     */
    private BigDecimal dispatchCalculation(List<CartItemDTO> cartItems, Discount discount, boolean isDryRun) {
        // 第一步先呼叫篩選器，撈出購物車裡「符合資格且還沒被用掉」的商品
        List<CartItemDTO> eligibleItems = filterEligibleItems(cartItems, discount, "Main");
        // 若找不到符合對象，直接回傳 0
        if (eligibleItems.isEmpty()) return BigDecimal.ZERO;

        // 取得活動是哪一種折扣代碼 (1~5)
        Integer typeId = discount.getDiscountType().getDiscountTypeId();
        
        // Switch 分流中心
        switch (typeId) {
            case 1: 
                // 百分比
                return calculationService.calculatePercentageDiscount(eligibleItems, discount, isDryRun);
            case 2: 
                // 定額滿減
                return calculationService.calculateFixedDiscount(eligibleItems, discount, isDryRun);
            case 3: { 
                // 買N送M (需額外撈取贈品)
                List<CartItemDTO> freeItems = filterEligibleItems(cartItems, discount, "Addon");
                return calculationService.calculateBuyNGetMDiscount(eligibleItems, freeItems, discount, isDryRun);
            }
            case 4: { 
                // 條件加購 (需額外撈取加購品)
                List<CartItemDTO> addonItems = filterEligibleItems(cartItems, discount, "Addon");
                return calculationService.calculateAddOnDiscount(eligibleItems, addonItems, discount, isDryRun);
            }
            case 5: 
                // 組合優惠價
                return calculationService.calculateBundleDiscount(eligibleItems, discount, isDryRun);
            default: 
                // 未知代碼防呆
                return BigDecimal.ZERO;
        }
    }

    /**
     * 商品篩選器：最底層負責決定誰能參加活動的警衛
     */
    private List<CartItemDTO> filterEligibleItems(List<CartItemDTO> cartItems, Discount discount, String role) { 
        // 先把購物車裡「還沒被標記過 (isProcessed == false)」的乾淨商品過濾出來
        List<CartItemDTO> availableItems = cartItems.stream()
                .filter(item -> !item.isProcessed())
                .collect(Collectors.toList());

        // 如果活動範圍是 1 (分類活動)
        if (discount.getScopeType() == 1) {
            // 從活動設定裡撈出所有目標分類的 ID
            Set<Integer> targetCatIds = discount.getDiscountCategories().stream()
                    .filter(dc -> role.equals(dc.getCategoryRole()))
                    .map(dc -> dc.getCategory().getCategoryId())
                    .collect(Collectors.toSet());
            
            // 回傳分類 ID 有中獎的商品
            return availableItems.stream()
                    .filter(item -> targetCatIds.contains(item.getCategoryId()))
                    .collect(Collectors.toList());
                    
        } else {
            // 如果活動範圍是 2 (單品活動)，從設定裡撈出目標商品 ID
            Set<Integer> targetProdIds = discount.getDiscountProducts().stream()
                    .filter(dp -> role.equals(dp.getProductRole()))
                    .map(dp -> dp.getProduct().getProductId())
                    .collect(Collectors.toSet());
            
            // 回傳商品 ID 有中獎的商品
            return availableItems.stream()
                    .filter(item -> targetProdIds.contains(item.getProductId()))
                    .collect(Collectors.toList());
        }
    }
}
