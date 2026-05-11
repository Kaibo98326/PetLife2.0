package com.petlife.service;



import org.springframework.stereotype.Service;
import com.petlife.model.Discount;
import com.petlife.repository.CartItemDTO; // 確保路徑與你的專案一致

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DiscountCalculationService {

    /**
     * Type 1: 百分比折扣 (例如：打 85 折)
     */
    public BigDecimal calculatePercentageDiscount(List<CartItemDTO> validItems, Discount discount) {
        BigDecimal totalAmount = calculateTotalAmount(validItems);
        BigDecimal minAmount = getMinAmount(discount);

        if (totalAmount.compareTo(minAmount) < 0) return BigDecimal.ZERO;

        BigDecimal discountValue = new BigDecimal(discount.getDiscountValue().toString());
        BigDecimal discountRate = BigDecimal.ONE.subtract(discountValue); 
        
        BigDecimal discountAmount = totalAmount.multiply(discountRate);
        markItemsAsProcessed(validItems);
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Type 2: 滿額折扣 (定額折抵，例如：滿 1000 折 100)
     */
    public BigDecimal calculateFixedDiscount(List<CartItemDTO> validItems, Discount discount) {
        BigDecimal totalAmount = calculateTotalAmount(validItems);
        BigDecimal minAmount = getMinAmount(discount);

        if (totalAmount.compareTo(minAmount) < 0) return BigDecimal.ZERO;

        BigDecimal discountAmount = new BigDecimal(discount.getDiscountValue().toString());
        if (discountAmount.compareTo(totalAmount) > 0) discountAmount = totalAmount;

        markItemsAsProcessed(validItems);
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Type 3: 買 N 送 M (例如：買 3 送 1)
     * ✨ 修正：自動扣掉最便宜的 M 件商品金額
     */
    public BigDecimal calculateBuyNGetMDiscount(List<CartItemDTO> validItems, Discount discount) {
        // 1. 將所有商品按數量展開成單價清單 (例如 3 件 $100 會變成 [100, 100, 100])
        List<BigDecimal> allUnitPrices = flattenItemsToUnitPrices(validItems);
        
        int buyN = discount.getBuyQuantity();
        int freeM = discount.getFreeQuantity();
        int setSize = buyN + freeM; // 一組需要多少件
        
        // 2. 計算總共符合幾組
        int sets = allUnitPrices.size() / setSize;
        if (sets == 0) return BigDecimal.ZERO;

        // 3. 排序單價 (從低到高)
        Collections.sort(allUnitPrices);

        // 4. ✨ 核心邏輯：從最便宜的開始挑選 (sets * freeM) 件作為折扣金額
        BigDecimal discountAmount = BigDecimal.ZERO;
        int itemsToFree = sets * freeM;
        
        for (int i = 0; i < itemsToFree; i++) {
            discountAmount = discountAmount.add(allUnitPrices.get(i));
        }

        markItemsAsProcessed(validItems);
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Type 4: 條件加購價 (滿 N 件，副項加購價)
     */
    public BigDecimal calculateAddOnDiscount(List<CartItemDTO> mainItems, List<CartItemDTO> addonItems, Discount discount) {
        int totalMainQty = mainItems.stream().mapToInt(CartItemDTO::getQuantity).sum();
        if (totalMainQty < discount.getBuyQuantity()) return BigDecimal.ZERO;
        if (addonItems.isEmpty()) return BigDecimal.ZERO;

        // 挑選最便宜的副商品來進行加購折抵
        List<BigDecimal> addonPrices = flattenItemsToUnitPrices(addonItems);
        Collections.sort(addonPrices);
        
        BigDecimal originalPrice = addonPrices.get(0); // 取最便宜的那一件
        BigDecimal addOnPrice = new BigDecimal(discount.getDiscountValue().toString());
        
        BigDecimal discountAmount = originalPrice.subtract(addOnPrice);
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;

        markItemsAsProcessed(mainItems);
        markItemsAsProcessed(addonItems);
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Type 5: 組合條件價 (例如：任選 3 件 50 元)
     * ✨ 修正：以最便宜的 N 件為一組計算原價差額
     */
    public BigDecimal calculateBundleDiscount(List<CartItemDTO> validItems, Discount discount) {
        List<BigDecimal> allUnitPrices = flattenItemsToUnitPrices(validItems);
        int requiredQty = discount.getBuyQuantity();
        
        int sets = allUnitPrices.size() / requiredQty;
        if (sets == 0) return BigDecimal.ZERO;

        // 排序單價 (低到高)
        Collections.sort(allUnitPrices);

        // 算出這些「即將被打包」商品的原價總和 (由低價往高價挑選 sets * requiredQty 件)
        BigDecimal originalTotalOfBundledItems = BigDecimal.ZERO;
        for (int i = 0; i < sets * requiredQty; i++) {
            originalTotalOfBundledItems = originalTotalOfBundledItems.add(allUnitPrices.get(i));
        }

        // 組合後的總價
        BigDecimal bundlePriceTotal = new BigDecimal(discount.getDiscountValue().toString()).multiply(new BigDecimal(sets));
        
        // 折扣金額 = 這些商品的原價 - 組合優惠價
        BigDecimal discountAmount = originalTotalOfBundledItems.subtract(bundlePriceTotal);

        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;

        markItemsAsProcessed(validItems);
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    // ==========================================
    // ⬇️ 共用工具與輔助方法 ⬇️
    // ==========================================

    /**
     * ✨ 輔助：將帶數量的商品清單展開為純單價清單
     */
    private List<BigDecimal> flattenItemsToUnitPrices(List<CartItemDTO> items) {
        List<BigDecimal> prices = new ArrayList<>();
        for (CartItemDTO item : items) {
            for (int i = 0; i < item.getQuantity(); i++) {
                prices.add(item.getPrice());
            }
        }
        return prices;
    }

    private BigDecimal calculateTotalAmount(List<CartItemDTO> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItemDTO item : items) {
            total = total.add(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }
        return total;
    }

    private BigDecimal getMinAmount(Discount discount) {
        return discount.getMinimumPurchaseAmount() != null 
                ? new BigDecimal(discount.getMinimumPurchaseAmount()) 
                : BigDecimal.ZERO;
    }

    private void markItemsAsProcessed(List<CartItemDTO> items) {
        for (CartItemDTO item : items) {
            item.setProcessed(true); // 標記為已被活動使用
        }
    }
}