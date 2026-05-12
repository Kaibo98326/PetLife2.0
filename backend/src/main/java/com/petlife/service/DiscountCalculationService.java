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
    
 // 對應 Engine 傳進來的內容
    public BigDecimal calculateBuyNGetMDiscount(List<CartItemDTO> mainItems, List<CartItemDTO> freeItems, Discount discount) {
        
        // 2. 核心：計算「買 N」的門檻是看 mainItems (主商品)
        List<BigDecimal> mainUnitPrices = flattenItemsToUnitPrices(mainItems);
        int buyN = discount.getBuyQuantity();
        int freeM = discount.getFreeQuantity();
        
        // 算出符合幾組活動 (例如買 3 送 1，看主商品湊到幾個 3)
        int sets = mainUnitPrices.size() / buyN; 
        if (sets == 0 || freeItems.isEmpty()) return BigDecimal.ZERO;

        // 3. 核心：要「送 M」的東西是從 freeItems (副商品) 裡面挑
        List<BigDecimal> freeUnitPrices = flattenItemsToUnitPrices(freeItems);
        
        // 排序副商品單價 (從低到高)，確保扣掉的是最便宜的贈品
        Collections.sort(freeUnitPrices);

        // 4. 計算折扣金額：最多能送幾件？ (組數 * 每組送幾件)
        // 但不能超過消費者實際買的副商品數量
        int maxItemsToFree = sets * freeM;
        int actualItemsToFree = Math.min(maxItemsToFree, freeUnitPrices.size());
        
        BigDecimal discountAmount = BigDecimal.ZERO;
        for (int i = 0; i < actualItemsToFree; i++) {
            discountAmount = discountAmount.add(freeUnitPrices.get(i));
        }

        // 5. 重要：把主副商品都標記為已處理，防止重複打折
        markItemsAsProcessed(mainItems);
        markItemsAsProcessed(freeItems);

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
 // new BigDecimal(discount.getMinimumPurchaseAmount())  直接回傳，把外面的 new BigDecimal() 拿掉
            ? discount.getMinimumPurchaseAmount() 
            : BigDecimal.ZERO;
    }

    private void markItemsAsProcessed(List<CartItemDTO> items) {
        for (CartItemDTO item : items) {
            item.setProcessed(true); // 標記為已被活動使用
        }
    }
}