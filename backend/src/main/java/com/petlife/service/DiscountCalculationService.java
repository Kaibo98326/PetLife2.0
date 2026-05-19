
package com.petlife.service;
import org.springframework.stereotype.Service;
import com.petlife.model.Discount;
import com.petlife.repository.CartItemDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class DiscountCalculationService {

    // 【百分比折扣】
    public BigDecimal calculatePercentageDiscount(List<CartItemDTO> validItems, Discount discount, boolean isDryRun) {
        BigDecimal totalAmount = calculateTotalAmount(validItems);
        BigDecimal minAmount = getMinAmount(discount);

        if (totalAmount.compareTo(minAmount) < 0) return BigDecimal.ZERO;

        BigDecimal discountRate = BigDecimal.ONE.subtract(new BigDecimal(discount.getDiscountValue().toString())); 
        BigDecimal discountAmount = totalAmount.multiply(discountRate);

        // --- 活動新增：判斷是否為假算 ---
        if (!isDryRun) {
            markItems(validItems, validItems.size());
            apportionDiscount(validItems, discountAmount); 
        }
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    // 【滿額折扣】
    public BigDecimal calculateFixedDiscount(List<CartItemDTO> validItems, Discount discount, boolean isDryRun) {
        BigDecimal totalAmount = calculateTotalAmount(validItems);
        BigDecimal minAmount = getMinAmount(discount);

        if (totalAmount.compareTo(minAmount) < 0) return BigDecimal.ZERO;

        BigDecimal discountAmount = new BigDecimal(discount.getDiscountValue().toString());
        if (discountAmount.compareTo(totalAmount) > 0) discountAmount = totalAmount;

        // --- 活動新增：判斷是否為假算 ---
        if (!isDryRun) {
            markItems(validItems, validItems.size());
            apportionDiscount(validItems, discountAmount); 
        }
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    // 【買 N 送 M】
    public BigDecimal calculateBuyNGetMDiscount(List<CartItemDTO> mainItems, List<CartItemDTO> freeItems, Discount discount, boolean isDryRun) {
        int buyN = discount.getBuyQuantity();
        int freeM = discount.getFreeQuantity();
        
        // ✨ 新增/修改：正確的套組門檻必須是 (買 N 件 + 送 M 件)
        int requiredPerSet = buyN + freeM; 
        
        // ✨ 新增/修改：用總件數來計算符合的組數 Math.floor(quantity / (X + Y))
        int sets = mainItems.size() / requiredPerSet; 
        if (sets == 0) return BigDecimal.ZERO; // 未達門檻直接回傳 0

        // 依價格由低到高排序，確保贈送的是最便宜的商品
        mainItems.sort((i1, i2) -> i1.getPrice().compareTo(i2.getPrice()));
        
        // ✨ 新增/修改：實際要折抵(免費)的件數 = 組數 * 贈送件數 M
        int actualItemsToFree = sets * freeM;
        
        BigDecimal discountAmount = BigDecimal.ZERO;
        for (int i = 0; i < actualItemsToFree; i++) {
            discountAmount = discountAmount.add(mainItems.get(i).getPrice());
        }

        // --- 活動新增：判斷是否為假算 ---
        if (!isDryRun) {
            // ✨ 新增/修改：將一整組 (N+M) 的商品標記為已處理
            List<CartItemDTO> markedItems = markItems(mainItems, sets * requiredPerSet);
            apportionDiscount(markedItems, discountAmount); 
        }
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    // 【條件加購價】
    public BigDecimal calculateAddOnDiscount(List<CartItemDTO> mainItems, List<CartItemDTO> addonItems, Discount discount, boolean isDryRun) {
        if (mainItems.size() < discount.getBuyQuantity() || addonItems.isEmpty()) return BigDecimal.ZERO;

        addonItems.sort((i1, i2) -> i1.getPrice().compareTo(i2.getPrice()));
        BigDecimal originalPrice = addonItems.get(0).getPrice(); 
        BigDecimal addOnPrice = new BigDecimal(discount.getDiscountValue().toString());
        BigDecimal discountAmount = originalPrice.subtract(addOnPrice);
        
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;

        // --- 活動新增：判斷是否為假算 ---
        if (!isDryRun) {
            markItems(mainItems, discount.getBuyQuantity());
            List<CartItemDTO> markedAddon = markItems(addonItems, 1);
            apportionDiscount(markedAddon, discountAmount); 
        }
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    // 【組合價】
    public BigDecimal calculateBundleDiscount(List<CartItemDTO> validItems, Discount discount, boolean isDryRun) {
        int requiredQty = discount.getBuyQuantity();
        int sets = validItems.size() / requiredQty;
        
        if (sets == 0) return BigDecimal.ZERO;

        validItems.sort((i1, i2) -> i1.getPrice().compareTo(i2.getPrice()));

        BigDecimal originalTotal = BigDecimal.ZERO;
        for (int i = 0; i < sets * requiredQty; i++) {
            originalTotal = originalTotal.add(validItems.get(i).getPrice());
        }

        BigDecimal bundleTotal = new BigDecimal(discount.getDiscountValue().toString()).multiply(new BigDecimal(sets));
        BigDecimal discountAmount = originalTotal.subtract(bundleTotal);

        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;

        // --- 活動新增：判斷是否為假算 ---
        if (!isDryRun) {
            List<CartItemDTO> markedItems = markItems(validItems, sets * requiredQty);
            apportionDiscount(markedItems, discountAmount); 
        }
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    // ==========================================
    // ⬇️ 輔助工具方法 ⬇️
    // ==========================================

    private List<CartItemDTO> markItems(List<CartItemDTO> items, int targetCount) {
        List<CartItemDTO> markedList = new ArrayList<>();
        for (int i = 0; i < targetCount; i++) {
            CartItemDTO item = items.get(i);
            item.setProcessed(true);
            markedList.add(item);
        }
        return markedList;
    }

    private void apportionDiscount(List<CartItemDTO> markedItems, BigDecimal totalDiscount) {
        if (totalDiscount.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal originalTotal = calculateTotalAmount(markedItems);
        if (originalTotal.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal accumulated = BigDecimal.ZERO;
        for (int i = 0; i < markedItems.size(); i++) {
            CartItemDTO item = markedItems.get(i);
            BigDecimal itemShare;
            if (i == markedItems.size() - 1) {
                itemShare = totalDiscount.subtract(accumulated);
            } else {
                itemShare = totalDiscount.multiply(item.getPrice()).divide(originalTotal, 0, RoundingMode.HALF_UP);
            }
            accumulated = accumulated.add(itemShare);
        }
    }

    private BigDecimal calculateTotalAmount(List<CartItemDTO> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItemDTO item : items) {
            total = total.add(item.getPrice());
        }
        return total;
    }
    
    private BigDecimal getMinAmount(Discount discount) { 
        return discount.getMinimumPurchaseAmount() != null ? discount.getMinimumPurchaseAmount() : BigDecimal.ZERO; 
    }
}