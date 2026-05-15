package com.petlife.service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.petlife.model.*;
import com.petlife.repository.*;

@Service
public class DiscountEngine {

    @Autowired
    private DiscountCalculationService calculationService;

    @Autowired
    private DiscountTemplateHelper templateHelper;

    public CartCalculateResponseDTO executeDiscount(List<CartItemDTO> originalItems, List<Discount> allActiveDiscounts) {
        // --- 活動新增：1. 攤平商品 (把數量拆成多筆單件商品) ---
        List<CartItemDTO> flattenedItems = flattenCartItems(originalItems);
        
        BigDecimal totalSaved = BigDecimal.ZERO;
        List<DiscountDetailDTO> appliedDetails = new ArrayList<>();

        // 2. 依照優先權分組 (單品 > 分類)
        List<Discount> productLevelDiscounts = allActiveDiscounts.stream()
                .filter(d -> d.getScopeType() == 2).collect(Collectors.toList());
        List<Discount> categoryLevelDiscounts = allActiveDiscounts.stream()
                .filter(d -> d.getScopeType() == 1).collect(Collectors.toList());

        // 【單品層級擇優】
        productLevelDiscounts.sort((d1, d2) -> {
            BigDecimal saved1 = dispatchCalculation(flattenedItems, d1, true); 
            BigDecimal saved2 = dispatchCalculation(flattenedItems, d2, true);
            return saved2.compareTo(saved1); 
        });

        for (Discount discount : productLevelDiscounts) {
            Set<Integer> beforeProcessedIds = flattenedItems.stream()
                .filter(CartItemDTO::isProcessed)
                .map(CartItemDTO::getItemId).collect(Collectors.toSet());

            BigDecimal saved = dispatchCalculation(flattenedItems, discount, false); 
            if (saved.compareTo(BigDecimal.ZERO) > 0) {
                totalSaved = totalSaved.add(saved);
                appliedDetails.add(new DiscountDetailDTO(discount.getDiscountName(), templateHelper.generateDiscountDetailText(discount), saved));
                
                String badgeText = templateHelper.generateAppliedText(discount);
                flattenedItems.stream()
                    .filter(i -> i.isProcessed() && !beforeProcessedIds.contains(i.getItemId()))
                    .forEach(i -> i.setAppliedDiscountText(badgeText));
            }
        }

        // 【分類層級擇優】
        categoryLevelDiscounts.sort((d1, d2) -> {
            BigDecimal saved1 = dispatchCalculation(flattenedItems, d1, true); 
            BigDecimal saved2 = dispatchCalculation(flattenedItems, d2, true);
            return saved2.compareTo(saved1); 
        });

        for (Discount discount : categoryLevelDiscounts) {
            Set<Integer> beforeProcessedIds = flattenedItems.stream()
                .filter(CartItemDTO::isProcessed)
                .map(CartItemDTO::getItemId).collect(Collectors.toSet());

            BigDecimal saved = dispatchCalculation(flattenedItems, discount, false); 
            if (saved.compareTo(BigDecimal.ZERO) > 0) {
                totalSaved = totalSaved.add(saved);
                appliedDetails.add(new DiscountDetailDTO(discount.getDiscountName(), templateHelper.generateDiscountDetailText(discount), saved));
                
                String badgeText = templateHelper.generateAppliedText(discount);
                flattenedItems.stream()
                    .filter(i -> i.isProcessed() && !beforeProcessedIds.contains(i.getItemId()))
                    .forEach(i -> i.setAppliedDiscountText(badgeText));
            }
        }

        // --- 活動新增：3. 計算門檻提醒並合併還原 ---
        calculateReminders(flattenedItems, allActiveDiscounts);
        List<CartItemDTO> mergedItems = mergeCartItems(flattenedItems);

        CartCalculateResponseDTO response = new CartCalculateResponseDTO();
        response.setDiscountAmount(totalSaved);
        response.setAppliedDiscounts(appliedDetails);
        response.setCartItems(mergedItems); 
        return response;
    }

    // --- 活動新增：攤平邏輯 ---
    private List<CartItemDTO> flattenCartItems(List<CartItemDTO> originalItems) {
        List<CartItemDTO> flattened = new ArrayList<>();
        for (CartItemDTO item : originalItems) {
            for (int i = 0; i < item.getQuantity(); i++) {
                CartItemDTO single = new CartItemDTO();
                single.setItemId(item.getItemId()); // 保持同款商品 ID 一致
                single.setProductId(item.getProductId());
                single.setCategoryId(item.getCategoryId());
                single.setPrice(item.getPrice());
                single.setQuantity(1); // 強制變 1
                flattened.add(single);
            }
        }
        return flattened;
    }

    // --- 活動新增：合併邏輯 (只要部分有折抵就帶標籤) ---
    private List<CartItemDTO> mergeCartItems(List<CartItemDTO> flattened) {
        Map<Integer, CartItemDTO> map = new LinkedHashMap<>();
        for (CartItemDTO f : flattened) {
            if (!map.containsKey(f.getItemId())) {
                map.put(f.getItemId(), f);
            } else {
                CartItemDTO existing = map.get(f.getItemId());
                existing.setQuantity(existing.getQuantity() + 1);
                // 只要子項目有標籤或提醒，就保留
                if (f.getAppliedDiscountText() != null) existing.setAppliedDiscountText(f.getAppliedDiscountText());
                if (f.getReminderText() != null) existing.setReminderText(f.getReminderText());
            }
        }
        return new ArrayList<>(map.values());
    }

    // --- 活動新增：補齊 Filter 邏輯 (對接 Discount 實體) ---
    private List<CartItemDTO> filterEligibleItems(List<CartItemDTO> items, Discount discount, String String_role) {
        return items.stream()
            .filter(i -> !i.isProcessed())
            .filter(i -> {
                if (discount.getScopeType() == 2) { // 單品
                    Set<Integer> pIds = discount.getDiscountProducts().stream()
                        .map(dp -> dp.getProduct().getProductId()).collect(Collectors.toSet());
                    return pIds.contains(i.getProductId());
                } else { // 分類
                    Set<Integer> cIds = discount.getDiscountCategories().stream()
                        .map(dc -> dc.getCategory().getCategoryId()).collect(Collectors.toSet());
                    return cIds.contains(i.getCategoryId());
                }
            })
            .collect(Collectors.toList());
    }

    private BigDecimal dispatchCalculation(List<CartItemDTO> cartItems, Discount discount, boolean isDryRun) {
        List<CartItemDTO> eligibleItems = filterEligibleItems(cartItems, discount, "Main");
        if (eligibleItems.isEmpty()) return BigDecimal.ZERO;
        Integer typeId = discount.getDiscountType().getDiscountTypeId();
        switch (typeId) {
            case 1: return calculationService.calculatePercentageDiscount(eligibleItems, discount, isDryRun);
            case 2: return calculationService.calculateFixedDiscount(eligibleItems, discount, isDryRun);
            case 3: return calculationService.calculateBuyNGetMDiscount(eligibleItems, filterEligibleItems(cartItems, discount, "Addon"), discount, isDryRun);
            case 4: return calculationService.calculateAddOnDiscount(eligibleItems, filterEligibleItems(cartItems, discount, "Addon"), discount, isDryRun);
            case 5: return calculationService.calculateBundleDiscount(eligibleItems, discount, isDryRun);
            default: return BigDecimal.ZERO;
        }
    }

    /**
     * 計算未達標門檻提醒 (% 數最高者優先顯示)
     * --- 活動新增 ---
     */
    private void calculateReminders(List<CartItemDTO> items, List<Discount> allActive) {
        // 遍歷每一件攤平後的商品
        for (CartItemDTO item : items) {
            // 1. 【防禦】如果這件商品已經被某個活動「真算」處理過並標記了，就不再顯示提醒
            if (item.isProcessed()) continue; 

            double bestProgress = -1; // 用於紀錄目前最高進度百分比
            String bestReminder = null; // 用於紀錄最高進度的提醒文字

            for (Discount discount : allActive) {
                // 2. 【資格過濾】檢查這件單一商品是否有資格參加該活動 (Main 商品)
                List<CartItemDTO> singleItemList = new ArrayList<>();
                singleItemList.add(item);
                if (filterEligibleItems(singleItemList, discount, "Main").isEmpty()) continue;

                // 3. 【進度計算】算出整個購物車中，符合該活動資格的所有「乾淨商品」總額/總件數
                List<CartItemDTO> eligibleList = filterEligibleItems(items, discount, "Main");
                BigDecimal totalAmt = BigDecimal.ZERO;
                int totalQty = 0;
                for (CartItemDTO e : eligibleList) {
                    totalAmt = totalAmt.add(e.getPrice());
                    totalQty += 1;
                }

                // 4. 【類型判斷】根據活動類型 (Type ID) 判斷門檻差距
                int typeId = discount.getDiscountType().getDiscountTypeId();
                double currentProgress = 0;
                String reminderText = "";

                // A. 金額型門檻 (Type 1: 百分比, Type 2: 滿額折)
                if (typeId == 1 || typeId == 2) {
                    BigDecimal minAmount = discount.getMinimumPurchaseAmount();
                    if (minAmount != null && minAmount.compareTo(BigDecimal.ZERO) > 0 && totalAmt.compareTo(minAmount) < 0) {
                        currentProgress = (totalAmt.doubleValue() / minAmount.doubleValue()) * 100;
                        // ✨ 新增/修改：百分比與滿額折維持 80% 才提醒
                        if (currentProgress >= 80) {
                            BigDecimal diffAmount = minAmount.subtract(totalAmt);
                            reminderText = templateHelper.generateReminderText(discount, diffAmount, 0);
                        }
                    }
                } 
                // B. 件數型門檻 (Type 3: 買送, Type 4: 加購, Type 5: 組合)
                else {
                    // ✨ 新增/修改：修復提醒文字顯示邏輯 (依據活動定義目標件數)
                    int targetQty = 0;
                    if (discount.getBuyQuantity() != null) {
                        targetQty = discount.getBuyQuantity();
                    }
                    
                    // 如果是買N送M，湊單門檻為 N + M
                    if (typeId == 3 && discount.getFreeQuantity() != null) {
                        targetQty += discount.getFreeQuantity();
                    }

                    if (targetQty > 0 && totalQty < targetQty) {
                        currentProgress = ((double) totalQty / targetQty) * 100;
                        // ✨ 新增/修改：修復提醒文字顯示邏輯 (移除 80% 限制，只要未達標一律無條件提醒)
                        int diffQty = targetQty - totalQty;
                        reminderText = templateHelper.generateReminderText(discount, BigDecimal.ZERO, diffQty);
                    }
                }

                // 5. 【擇優提醒】如果這個活動的進度比之前的更高，就取代它 (顯示最接近達標的)
                // ✨ 新增/修改：修復提醒文字顯示邏輯 (改為 >=，確保 currentProgress 為 0 時也能大於 -1 並寫入)
                if (currentProgress >= bestProgress && reminderText != null && !reminderText.isEmpty()) {
                    bestProgress = currentProgress;
                    bestReminder = reminderText;
                }
            }

            // 6. 【貼上標籤】將贏得競爭的提醒文字存入 DTO，供後續合併並傳回前端
            if (bestReminder != null) {
                item.setReminderText(bestReminder);
            }
        }
    }
}