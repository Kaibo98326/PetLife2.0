package com.petlife.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.petlife.model.Discount;

import com.petlife.repository.CartItemDTO; // 確保路徑與你的 DTO 一致


@Service
public class DiscountEngine {

    @Autowired
    private DiscountCalculationService calculationService;

    /**
     * 執行整台購物車的折扣計算 (核心入口)
     * @param cartItems 原始購物車內容
     * @param allActiveDiscounts 從資料庫撈出的所有「進行中」活動
     * @return 總共折扣了多少錢
     */
    public BigDecimal executeDiscount(List<CartItemDTO> cartItems, List<Discount> allActiveDiscounts) {
        BigDecimal totalSaved = BigDecimal.ZERO;

        // 1. 依照優先權分組：單品活動 (Scope 2) 與 分類活動 (Scope 1)
        List<Discount> productLevelDiscounts = allActiveDiscounts.stream()
                .filter(d -> d.getScopeType() == 2) 
                .collect(Collectors.toList());

        List<Discount> categoryLevelDiscounts = allActiveDiscounts.stream()
                .filter(d -> d.getScopeType() == 1) 
                .collect(Collectors.toList());

        // --- 第一梯次：執行「單品」活動 (優先權最高) ---
        for (Discount discount : productLevelDiscounts) {
            BigDecimal saved = dispatchCalculation(cartItems, discount);
            totalSaved = totalSaved.add(saved);
        }

        // --- 第二梯次：執行「分類」活動 (單品不玩的，分類才接手) ---
        for (Discount discount : categoryLevelDiscounts) {
            BigDecimal saved = dispatchCalculation(cartItems, discount);
            totalSaved = totalSaved.add(saved);
        }

        return totalSaved;
    }

    /**
     *  邏輯分發中心：根據活動類型 (Type 1~5) 導向正確的計算方法
     */
    private BigDecimal dispatchCalculation(List<CartItemDTO> cartItems, Discount discount) {
        // 1. 篩選出「符合此活動範圍」且「尚未被處理過」的商品
        List<CartItemDTO> eligibleItems = filterEligibleItems(cartItems, discount, "Main");
        
        if (eligibleItems.isEmpty()) return BigDecimal.ZERO;

        // 2. 依照 DiscountType ID 進行分流
        Integer typeId = discount.getDiscountType().getDiscountTypeId();
        
        switch (typeId) {
            case 1: // 百分比折扣
                return calculationService.calculatePercentageDiscount(eligibleItems, discount);
            case 2: // 滿額折扣 (定額)
                return calculationService.calculateFixedDiscount(eligibleItems, discount);
            case 3: { // 買 N 送 M 
                List<CartItemDTO> freeItems = filterEligibleItems(cartItems, discount, "Addon");
                return calculationService.calculateBuyNGetMDiscount(eligibleItems, freeItems, discount);
            }
            case 4: { // 條件加購價 
                List<CartItemDTO> addonItems = filterEligibleItems(cartItems, discount, "Addon");
                return calculationService.calculateAddOnDiscount(eligibleItems, addonItems, discount);
            }
            case 5: // 組合條件價
                return calculationService.calculateBundleDiscount(eligibleItems, discount);
            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * ✨ 商品篩選器：根據 ScopeType 與 Role (Main/Addon) 撈出符合資格的商品
     */
    private List<CartItemDTO> filterEligibleItems(List<CartItemDTO> cartItems, Discount discount, String role) {
        // 排除掉已經被其他活動處理過的商品 (排他性)
        List<CartItemDTO> availableItems = cartItems.stream()
                .filter(item -> !item.isProcessed())
                .collect(Collectors.toList());

        if (discount.getScopeType() == 1) {
            // 分類範圍：檢查商品分類 ID
            Set<Integer> targetCatIds = discount.getDiscountCategories().stream()
                    .filter(dc -> role.equals(dc.getCategoryRole()))
                    .map(dc -> dc.getCategory().getCategoryId())
                    .collect(Collectors.toSet());
            
            return availableItems.stream()
                    .filter(item -> targetCatIds.contains(item.getCategoryId()))
                    .collect(Collectors.toList());
                    
        } else {
            // 單品範圍：檢查商品 ID
            Set<Integer> targetProdIds = discount.getDiscountProducts().stream()
                    .filter(dp -> role.equals(dp.getProductRole()))
                    .map(dp -> dp.getProduct().getProductId())
                    .collect(Collectors.toSet());
            
            return availableItems.stream()
                    .filter(item -> targetProdIds.contains(item.getProductId()))
                    .collect(Collectors.toList());
        }
    }
}
