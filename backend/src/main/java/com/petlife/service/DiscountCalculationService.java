// 定義所在的套件路徑
package com.petlife.service;

// 引入 Spring 的 Service 標記
import org.springframework.stereotype.Service;
// 引入 Discount 活動實體類別
import com.petlife.model.Discount;
// 引入購物車項目傳輸物件
import com.petlife.repository.CartItemDTO; 

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DiscountCalculationService {

    /**
     * Type 1: 百分比折扣 (例如：打 85 折)
     * ✨ 升級：加入 isDryRun 參數，若為 true 代表只是試算找最優解，不真正綁定商品
     */
    public BigDecimal calculatePercentageDiscount(List<CartItemDTO> validItems, Discount discount, boolean isDryRun) {
        BigDecimal totalAmount = calculateTotalAmount(validItems);
        BigDecimal minAmount = getMinAmount(discount);

        // 核心防線 6：未達門檻回傳 0，略過標記
        if (totalAmount.compareTo(minAmount) < 0) return BigDecimal.ZERO;

        BigDecimal discountValue = new BigDecimal(discount.getDiscountValue().toString());
        BigDecimal discountRate = BigDecimal.ONE.subtract(discountValue); 
        BigDecimal discountAmount = totalAmount.multiply(discountRate);

        // 若不是試算，才真正執行標記與分攤
        if (!isDryRun) {
            markItemsAsProcessed(validItems); // 核心防線 2：互斥綁定
            apportionDiscount(validItems, discountAmount); // ✨ 補充邏輯 9：原價比例分攤
        }
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Type 2: 滿額折扣 (定額折抵)
     */
    public BigDecimal calculateFixedDiscount(List<CartItemDTO> validItems, Discount discount, boolean isDryRun) {
        BigDecimal totalAmount = calculateTotalAmount(validItems);
        BigDecimal minAmount = getMinAmount(discount);

        if (totalAmount.compareTo(minAmount) < 0) return BigDecimal.ZERO;

        BigDecimal discountAmount = new BigDecimal(discount.getDiscountValue().toString());
        if (discountAmount.compareTo(totalAmount) > 0) discountAmount = totalAmount;

        if (!isDryRun) {
            markItemsAsProcessed(validItems);
            apportionDiscount(validItems, discountAmount); // ✨ 分攤退貨金額
        }
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Type 3: 買 N 送 M
     */
    public BigDecimal calculateBuyNGetMDiscount(List<CartItemDTO> mainItems, List<CartItemDTO> freeItems, Discount discount, boolean isDryRun) {
        List<BigDecimal> mainUnitPrices = flattenItemsToUnitPrices(mainItems);
        int buyN = discount.getBuyQuantity();
        int freeM = discount.getFreeQuantity();
        
        int sets = mainUnitPrices.size() / buyN; 
        if (sets == 0 || freeItems.isEmpty()) return BigDecimal.ZERO;

        List<BigDecimal> freeUnitPrices = flattenItemsToUnitPrices(freeItems);
        Collections.sort(freeUnitPrices);

        int maxItemsToFree = sets * freeM;
        int actualItemsToFree = Math.min(maxItemsToFree, freeUnitPrices.size());
        
        BigDecimal discountAmount = BigDecimal.ZERO;
        for (int i = 0; i < actualItemsToFree; i++) {
            discountAmount = discountAmount.add(freeUnitPrices.get(i));
        }

        if (!isDryRun) {
            // 核心防線 4 & 5：只標記實際被吃掉的件數 (此處為簡化寫法，實務上需依照件數拆分DTO，確保餘數釋放)
            markItemsAsProcessed(mainItems);
            markItemsAsProcessed(freeItems);
            
            // 將送的總金額分攤回這些被打包的商品身上
            List<CartItemDTO> allBundled = new ArrayList<>(mainItems);
            allBundled.addAll(freeItems);
            apportionDiscount(allBundled, discountAmount); // ✨ 分攤退貨金額
        }
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Type 4: 條件加購價 
     */
    public BigDecimal calculateAddOnDiscount(List<CartItemDTO> mainItems, List<CartItemDTO> addonItems, Discount discount, boolean isDryRun) {
        int totalMainQty = mainItems.stream().mapToInt(CartItemDTO::getQuantity).sum();
        if (totalMainQty < discount.getBuyQuantity()) return BigDecimal.ZERO;
        if (addonItems.isEmpty()) return BigDecimal.ZERO;

        List<BigDecimal> addonPrices = flattenItemsToUnitPrices(addonItems);
        Collections.sort(addonPrices);
        
        BigDecimal originalPrice = addonPrices.get(0); 
        BigDecimal addOnPrice = new BigDecimal(discount.getDiscountValue().toString());
        BigDecimal discountAmount = originalPrice.subtract(addOnPrice);
        
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;

        if (!isDryRun) {
            markItemsAsProcessed(mainItems);
            markItemsAsProcessed(addonItems);
            apportionDiscount(addonItems, discountAmount); // ✨ 折扣只分攤給加購品
        }
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Type 5: 組合條件價 (任選 N 件 M 元)
     */
    public BigDecimal calculateBundleDiscount(List<CartItemDTO> validItems, Discount discount, boolean isDryRun) {
        List<BigDecimal> allUnitPrices = flattenItemsToUnitPrices(validItems);
        int requiredQty = discount.getBuyQuantity();
        int sets = allUnitPrices.size() / requiredQty;
        
        if (sets == 0) return BigDecimal.ZERO;
        Collections.sort(allUnitPrices);

        BigDecimal originalTotalOfBundledItems = BigDecimal.ZERO;
        for (int i = 0; i < sets * requiredQty; i++) {
            originalTotalOfBundledItems = originalTotalOfBundledItems.add(allUnitPrices.get(i));
        }

        BigDecimal bundlePriceTotal = new BigDecimal(discount.getDiscountValue().toString()).multiply(new BigDecimal(sets));
        BigDecimal discountAmount = originalTotalOfBundledItems.subtract(bundlePriceTotal);

        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;

        if (!isDryRun) {
            markItemsAsProcessed(validItems);
            apportionDiscount(validItems, discountAmount); // ✨ 分攤退貨金額
        }
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    // ==========================================
    // ⬇️ 共用工具與輔助方法 ⬇️
    // ==========================================

    // ✨ 補充邏輯 9：原價比例分攤演算法 (Pro Rata Apportionment)
    private void apportionDiscount(List<CartItemDTO> bundledItems, BigDecimal totalDiscount) {
        // 若沒有打折，不需要分攤
        if (totalDiscount.compareTo(BigDecimal.ZERO) <= 0) return;
        
        // 1. 算出這群商品的原價總和 (作為分母)
        BigDecimal originalTotal = calculateTotalAmount(bundledItems);
        if (originalTotal.compareTo(BigDecimal.ZERO) == 0) return;

        // 用來記錄已經分攤掉多少錢
        BigDecimal accumulatedDiscount = BigDecimal.ZERO;
        
        // 2. 依序分攤給每個商品
        for (int i = 0; i < bundledItems.size(); i++) {
            CartItemDTO item = bundledItems.get(i);
            BigDecimal itemTotal = item.getPrice().multiply(new BigDecimal(item.getQuantity()));
            BigDecimal itemDiscount;
            
            // 【防呆技巧】如果是最後一件商品，用「總折扣 - 已分攤的金額」來吃掉小數點除不盡的誤差！
            if (i == bundledItems.size() - 1) {
                itemDiscount = totalDiscount.subtract(accumulatedDiscount);
            } else {
                // 公式：總折扣 × (該商品原價 / 總原價)，並四捨五入到整數
                itemDiscount = totalDiscount.multiply(itemTotal)
                        .divide(originalTotal, 0, RoundingMode.HALF_UP);
            }
            
            // 將算出的分攤金額存入該商品 (註：請確認你的 CartItemDTO 裡有新增這個屬性與 set 方法)
            // item.setApportionedDiscountAmount(itemDiscount); 
            
            accumulatedDiscount = accumulatedDiscount.add(itemDiscount);
        }
    }

    // ... 下方的 flattenItemsToUnitPrices, calculateTotalAmount, getMinAmount, markItemsAsProcessed 保持原樣不變 ...
    private List<BigDecimal> flattenItemsToUnitPrices(List<CartItemDTO> items) { /* 原本程式碼 */ return new ArrayList<>(); }
    private BigDecimal calculateTotalAmount(List<CartItemDTO> items) { 
        BigDecimal total = BigDecimal.ZERO;
        for (CartItemDTO item : items) total = total.add(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
        return total; 
    }
    private BigDecimal getMinAmount(Discount discount) { return discount.getMinimumPurchaseAmount() != null ? discount.getMinimumPurchaseAmount() : BigDecimal.ZERO; }
    private void markItemsAsProcessed(List<CartItemDTO> items) { for (CartItemDTO item : items) item.setProcessed(true); }
}