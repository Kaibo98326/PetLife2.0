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

	// ==========================================
    // Type 1: 百分比折扣 (例如：打 85 折)
    // ==========================================
    
    // 計算百分比折扣的方法，傳入符合資格的商品清單與活動規則
    public BigDecimal calculatePercentageDiscount(List<CartItemDTO> validItems, Discount discount) {
        // 呼叫輔助方法，算出這些符合資格的商品「總金額」
        BigDecimal totalAmount = calculateTotalAmount(validItems);
        // 呼叫輔助方法，取得該活動的「最低滿額門檻」
        BigDecimal minAmount = getMinAmount(discount);

        // 【防護防線】如果商品總額 < 最低門檻，直接回傳折扣 0 元
        if (totalAmount.compareTo(minAmount) < 0) return BigDecimal.ZERO;

        // 取得活動設定的折扣值 (例如 0.65)，並轉為 BigDecimal 型態
        BigDecimal discountValue = new BigDecimal(discount.getDiscountValue().toString());
        // 計算折抵比例：用 1 去減掉折扣值 (例如 1 - 0.65 = 0.35 折扣率)
        BigDecimal discountRate = BigDecimal.ONE.subtract(discountValue); 
        
        // 算出總共可以折多少錢：總金額 × 折抵比例
        BigDecimal discountAmount = totalAmount.multiply(discountRate);
        // 幫這些商品貼上「已參加過活動」的標籤，防止被其他活動重複折抵
        markItemsAsProcessed(validItems);
        // 回傳折扣金額，並設定小數點為 0 位，採四捨五入
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    // ==========================================
    // Type 2: 滿額折扣 (定額折抵，例如：滿 1000 折 100)
    // ==========================================
    
    // 計算定額折抵的方法
    public BigDecimal calculateFixedDiscount(List<CartItemDTO> validItems, Discount discount) {
        // 算出這些符合資格的商品「總金額」
        BigDecimal totalAmount = calculateTotalAmount(validItems);
        // 取得該活動的「最低滿額門檻」
        BigDecimal minAmount = getMinAmount(discount);

        // 【防護防線】如果商品總額 < 最低門檻，直接回傳折扣 0 元
        if (totalAmount.compareTo(minAmount) < 0) return BigDecimal.ZERO;

        // 取得活動設定的固定折抵金額 (例如 100)
        BigDecimal discountAmount = new BigDecimal(discount.getDiscountValue().toString());
        // 防呆機制：如果折抵金額大於商品總額，最多只能折到商品總額 (不能倒貼錢給客人)
        if (discountAmount.compareTo(totalAmount) > 0) discountAmount = totalAmount;

        // 幫商品貼上「已處理」標籤
        markItemsAsProcessed(validItems);
        // 回傳折扣金額，四捨五入至整數
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    // ==========================================
    // Type 3: 買 N 送 M (例如：買 3 送 1)
    // ==========================================
    
    // 計算買 N 送 M 的折扣方法，需傳入主商品與副商品清單
    public BigDecimal calculateBuyNGetMDiscount(List<CartItemDTO> mainItems, List<CartItemDTO> freeItems, Discount discount) {
        
        // 核心邏輯：把主商品展開成「一件一件」的單價清單
        List<BigDecimal> mainUnitPrices = flattenItemsToUnitPrices(mainItems);
        // 取得活動設定的主商品需要「買幾件」(N)
        int buyN = discount.getBuyQuantity();
        // 取得活動設定的副商品「送幾件」(M)
        int freeM = discount.getFreeQuantity();
        
        // 計算主商品的數量可以湊成「幾組」符合活動的條件
        int sets = mainUnitPrices.size() / buyN; 
        // 如果連 1 組都湊不到，或是購物車裡根本沒放要送的副商品，回傳 0 元折扣
        if (sets == 0 || freeItems.isEmpty()) return BigDecimal.ZERO;

        // 核心邏輯：把副(贈)商品展開成「一件一件」的單價清單
        List<BigDecimal> freeUnitPrices = flattenItemsToUnitPrices(freeItems);
        
        // 對副商品的單價進行排序 (從最便宜排到最貴)，確保公司是送最便宜的
        Collections.sort(freeUnitPrices);

        // 計算理論上總共可以送幾件 (組數 × 每組送的件數)
        int maxItemsToFree = sets * freeM;
        // 防呆：如果客人購物車裡的副商品數量，比我們能送的數量還少，就以客人拿的數量為準
        int actualItemsToFree = Math.min(maxItemsToFree, freeUnitPrices.size());
        
        // 準備一個變數來累加要折抵(送)的金額
        BigDecimal discountAmount = BigDecimal.ZERO;
        // 跑迴圈，從最便宜的副商品開始扣錢
        for (int i = 0; i < actualItemsToFree; i++) {
            // 把該副商品的單價加進折扣總額中
            discountAmount = discountAmount.add(freeUnitPrices.get(i));
        }

        // 把主商品標記為「已處理」
        markItemsAsProcessed(mainItems);
        // 把副商品標記為「已處理」
        markItemsAsProcessed(freeItems);

        // 回傳折扣金額，四捨五入至整數
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    // ==========================================
    // Type 4: 條件加購價 (滿 N 件，副項加購價)
    // ==========================================
    
    // 計算加購價折扣的方法
    public BigDecimal calculateAddOnDiscount(List<CartItemDTO> mainItems, List<CartItemDTO> addonItems, Discount discount) {
        // 算出主商品的總數量
        int totalMainQty = mainItems.stream().mapToInt(CartItemDTO::getQuantity).sum();
        // 如果主商品總數量 < 活動要求的數量，回傳 0 元折扣
        if (totalMainQty < discount.getBuyQuantity()) return BigDecimal.ZERO;
        // 如果購物車裡沒有放要加購的副商品，回傳 0 元折扣
        if (addonItems.isEmpty()) return BigDecimal.ZERO;

        // 將副商品展開成單價清單
        List<BigDecimal> addonPrices = flattenItemsToUnitPrices(addonItems);
        // 針對副商品單價進行由低到高排序
        Collections.sort(addonPrices);
        
        // 取得最便宜的那一件副商品原價
        BigDecimal originalPrice = addonPrices.get(0); 
        // 取得活動設定的「優惠加購價」(例如 50 元)
        BigDecimal addOnPrice = new BigDecimal(discount.getDiscountValue().toString());
        
        // 算出我們要幫客人折多少錢：原價 - 加購價
        BigDecimal discountAmount = originalPrice.subtract(addOnPrice);
        // 防呆：如果原價比加購價還便宜，代表折出來是負數，就回傳 0 元避免倒貼
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;

        // 標記主商品為已處理
        markItemsAsProcessed(mainItems);
        // 標記副商品為已處理
        markItemsAsProcessed(addonItems);
        // 回傳折扣金額，四捨五入至整數
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    // ==========================================
    // Type 5: 組合條件價 (例如：任選 3 件 500 元)
    // ==========================================
    
    // 計算組合條件價的方法
    public BigDecimal calculateBundleDiscount(List<CartItemDTO> validItems, Discount discount) {
        // 將所有符合資格的商品展開成單價清單
        List<BigDecimal> allUnitPrices = flattenItemsToUnitPrices(validItems);
        // 取得活動要求的「任選幾件」(N)
        int requiredQty = discount.getBuyQuantity();
        
        // 計算總共湊滿了「幾組」組合價
        int sets = allUnitPrices.size() / requiredQty;
        // 如果連 1 組都沒湊滿，回傳 0 元折扣
        if (sets == 0) return BigDecimal.ZERO;

        // 針對這些商品的單價進行排序 (由低到高)
        Collections.sort(allUnitPrices);

        // 準備一個變數，用來計算被挑走去配成組合的商品「原本總共要多少錢」
        BigDecimal originalTotalOfBundledItems = BigDecimal.ZERO;
        // 跑迴圈，從最便宜的開始挑，總共挑出 (組數 × 每組件數) 件
        for (int i = 0; i < sets * requiredQty; i++) {
            // 累加這些原價
            originalTotalOfBundledItems = originalTotalOfBundledItems.add(allUnitPrices.get(i));
        }

        // 計算客人實際要付的「組合優惠總價」(例如 2 組 × 500 元 = 1000 元)
        BigDecimal bundlePriceTotal = new BigDecimal(discount.getDiscountValue().toString()).multiply(new BigDecimal(sets));
        
        // 算出要幫客人折多少錢：原價總和 - 組合優惠總價
        BigDecimal discountAmount = originalTotalOfBundledItems.subtract(bundlePriceTotal);

        // 防呆：如果原本的總價比組合價還便宜 (折扣是負數)，回傳 0 元
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;

        // 標記這些商品為已處理
        markItemsAsProcessed(validItems);
        // 回傳折扣金額，四捨五入至整數
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    // ==========================================
    // ⬇️ 共用工具與輔助方法 ⬇️
    // ==========================================

    // 輔助方法：將帶有數量的商品清單，暴力展開為一維的「純單價清單」
    private List<BigDecimal> flattenItemsToUnitPrices(List<CartItemDTO> items) {
        // 建立一個裝 BigDecimal 的空陣列
        List<BigDecimal> prices = new ArrayList<>();
        // 遍歷每一個購物車項目
        for (CartItemDTO item : items) {
            // 根據該項目的購買數量，跑幾次迴圈
            for (int i = 0; i < item.getQuantity(); i++) {
                // 把該商品的單價塞進陣列裡
                prices.add(item.getPrice());
            }
        }
        // 回傳展開後的單價陣列
        return prices;
    }

    // 輔助方法：計算傳入商品清單的「真實總金額」(單價 × 數量)
    private BigDecimal calculateTotalAmount(List<CartItemDTO> items) {
        // 初始化總計為 0
        BigDecimal total = BigDecimal.ZERO;
        // 遍歷所有商品
        for (CartItemDTO item : items) {
            // 單價 × 數量後，累加到總計中
            total = total.add(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }
        // 回傳總計金額
        return total;
    }

    // 輔助方法：安全地取得活動的最低消費門檻 (防 Null 崩潰)
    private BigDecimal getMinAmount(Discount discount) {
        // 檢查活動物件裡的門檻欄位是否不是 null
        return discount.getMinimumPurchaseAmount() != null
            // 如果不是 null，直接回傳資料庫存的門檻值
            ? discount.getMinimumPurchaseAmount() 
            // 如果是 null (資料異常)，預設回傳 0 元門檻
            : BigDecimal.ZERO;
    }

    // 輔助方法：為商品掛上已處理標籤
    private void markItemsAsProcessed(List<CartItemDTO> items) {
        // 遍歷傳入的商品
        for (CartItemDTO item : items) {
            // 呼叫 setProcessed 將狀態改為 true
            item.setProcessed(true); 
        }
    }
}