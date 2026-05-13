package com.petlife.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.petlife.model.Discount;
import com.petlife.repository.CartItemDTO; 

// --- 活動新增開始 ---
import com.petlife.repository.CartCalculateResponseDTO;
import com.petlife.repository.DiscountDetailDTO;
// --- 活動新增結束 ---

@Service
public class DiscountEngine {

    @Autowired
    private DiscountCalculationService calculationService;

    // --- 活動新增開始 ---
    @Autowired
    private DiscountTemplateHelper templateHelper;
    // --- 活動新增結束 ---

    // --- 活動新增開始：修改回傳型別 ---
    public CartCalculateResponseDTO executeDiscount(List<CartItemDTO> cartItems, List<Discount> allActiveDiscounts) {
        BigDecimal totalSaved = BigDecimal.ZERO;
        List<DiscountDetailDTO> appliedDetails = new ArrayList<>();
    // --- 活動新增結束 ---

        // 1. 依照優先權分組 (核心防線 1：單品 > 分類)
        List<Discount> productLevelDiscounts = allActiveDiscounts.stream()
                .filter(d -> d.getScopeType() == 2).collect(Collectors.toList());

        List<Discount> categoryLevelDiscounts = allActiveDiscounts.stream()
                .filter(d -> d.getScopeType() == 1).collect(Collectors.toList());

        // ==============================================================
        //補充邏輯 7：同層級最優解 (Intra-Level Best Offer)
        // ==============================================================
        
        productLevelDiscounts.sort((d1, d2) -> {
            BigDecimal saved1 = dispatchCalculation(cartItems, d1, true); 
            BigDecimal saved2 = dispatchCalculation(cartItems, d2, true);
            return saved2.compareTo(saved1); 
        });

        for (Discount discount : productLevelDiscounts) {
            // --- 活動新增開始：快照比對前 ---
            Set<Integer> beforeProcessedIds = cartItems.stream()
                .filter(CartItemDTO::isProcessed)
                .map(CartItemDTO::getItemId)
                .collect(Collectors.toSet());
            // --- 活動新增結束 ---

            BigDecimal saved = dispatchCalculation(cartItems, discount, false); 
            
            if (saved.compareTo(BigDecimal.ZERO) > 0) {
                totalSaved = totalSaved.add(saved);
                
                // --- 活動新增開始：寫入雙重名稱與貼綠標 ---
                String detailText = templateHelper.generateDiscountDetailText(discount);
                appliedDetails.add(new DiscountDetailDTO(discount.getDiscountName(), detailText, saved));
                
                String badgeText = templateHelper.generateAppliedText(discount);
                cartItems.stream()
                    .filter(CartItemDTO::isProcessed)
                    .filter(item -> !beforeProcessedIds.contains(item.getItemId())) 
                    .forEach(item -> item.setAppliedDiscountText(badgeText)); 
                // --- 活動新增結束 ---
            }
        }

        categoryLevelDiscounts.sort((d1, d2) -> {
            BigDecimal saved1 = dispatchCalculation(cartItems, d1, true); 
            BigDecimal saved2 = dispatchCalculation(cartItems, d2, true);
            return saved2.compareTo(saved1); 
        });

        for (Discount discount : categoryLevelDiscounts) {
            // --- 活動新增開始：快照比對前 ---
            Set<Integer> beforeProcessedIds = cartItems.stream()
                .filter(CartItemDTO::isProcessed)
                .map(CartItemDTO::getItemId)
                .collect(Collectors.toSet());
            // --- 活動新增結束 ---

            BigDecimal saved = dispatchCalculation(cartItems, discount, false); 
            
            if (saved.compareTo(BigDecimal.ZERO) > 0) {
                totalSaved = totalSaved.add(saved);
                
                // --- 活動新增開始：寫入雙重名稱與貼綠標 ---
                String detailText = templateHelper.generateDiscountDetailText(discount);
                appliedDetails.add(new DiscountDetailDTO(discount.getDiscountName(), detailText, saved));
                
                String badgeText = templateHelper.generateAppliedText(discount);
                cartItems.stream()
                    .filter(CartItemDTO::isProcessed)
                    .filter(item -> !beforeProcessedIds.contains(item.getItemId()))
                    .forEach(item -> item.setAppliedDiscountText(badgeText));
                // --- 活動新增結束 ---
            }
        }

        // ==============================================================
        // ✨ 補充邏輯 8：最低結帳金額保護 (Minimum Checkout Protection)
        // ==============================================================
        
        BigDecimal cartTotalAmount = BigDecimal.ZERO;
        for(CartItemDTO item : cartItems) {
            cartTotalAmount = cartTotalAmount.add(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }
        
        BigDecimal minPayable = BigDecimal.ONE; 
        BigDecimal maxAllowedDiscount = cartTotalAmount.subtract(minPayable);

        if (totalSaved.compareTo(maxAllowedDiscount) > 0) {
            totalSaved = maxAllowedDiscount;
        }

        totalSaved = totalSaved.compareTo(BigDecimal.ZERO) > 0 ? totalSaved : BigDecimal.ZERO;

        // --- 活動新增開始：計算未達標提醒，並回傳 DTO ---
        calculateReminders(cartItems, allActiveDiscounts);
        
        CartCalculateResponseDTO response = new CartCalculateResponseDTO();
        response.setDiscountAmount(totalSaved);
        response.setAppliedDiscounts(appliedDetails);
        response.setCartItems(cartItems); 
        return response;
        // --- 活動新增結束 ---
    }

    // --- 活動新增開始：未達標提醒計算 ---
    private void calculateReminders(List<CartItemDTO> cartItems, List<Discount> allActiveDiscounts) {
        for (CartItemDTO item : cartItems) {
            if (item.isProcessed()) continue; 
            
            double bestProgress = -1;
            String bestReminder = null;
            
            for (Discount discount : allActiveDiscounts) {
                List<CartItemDTO> dummyList = new ArrayList<>();
                dummyList.add(item);
                if (filterEligibleItems(dummyList, discount, "Main").isEmpty()) continue;
                
                List<CartItemDTO> eligibleList = filterEligibleItems(cartItems, discount, "Main");
                BigDecimal totalAmt = BigDecimal.ZERO;
                int totalQty = 0;
                for (CartItemDTO e : eligibleList) {
                    totalAmt = totalAmt.add(e.getPrice().multiply(new BigDecimal(e.getQuantity())));
                    totalQty += e.getQuantity();
                }
                
                int type = discount.getDiscountType().getDiscountTypeId();
                double progress = 0;
                String reminderText = "";
                
                if (type == 1 || type == 2) {
                    BigDecimal min = discount.getMinimumPurchaseAmount();
                    if (min != null && min.compareTo(BigDecimal.ZERO) > 0 && totalAmt.compareTo(min) < 0) {
                        progress = totalAmt.doubleValue() / min.doubleValue() * 100;
                        BigDecimal diff = min.subtract(totalAmt);
                        reminderText = templateHelper.generateReminderText(discount, diff, 0);
                    }
                } else {
                    Integer minQty = discount.getBuyQuantity();
                    if (minQty != null && minQty > 0 && totalQty < minQty) {
                        progress = ((double) totalQty / minQty) * 100;
                        int diff = minQty - totalQty;
                        reminderText = templateHelper.generateReminderText(discount, BigDecimal.ZERO, diff);
                    }
                }
                
                if (progress > bestProgress && !reminderText.isEmpty()) {
                    bestProgress = progress;
                    bestReminder = reminderText;
                }
            }
            if (bestReminder != null) item.setReminderText(bestReminder);
        }
    }
    // --- 活動新增結束 ---

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

    private List<CartItemDTO> filterEligibleItems(List<CartItemDTO> cartItems, Discount discount, String role) { 
        // 原本的程式碼保持不變...
        return cartItems.stream().filter(item -> !item.isProcessed()).collect(Collectors.toList()); 
    }
}