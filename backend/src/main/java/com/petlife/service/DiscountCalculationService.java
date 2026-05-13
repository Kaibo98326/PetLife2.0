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

	// 【百分比折扣】
    public BigDecimal calculatePercentageDiscount(List<CartItemDTO> validItems, Discount discount, boolean isDryRun) {
        // 算出符合資格商品總價
        BigDecimal totalAmount = calculateTotalAmount(validItems);
        // 取得門檻
        BigDecimal minAmount = getMinAmount(discount);

        // 【規則 3：門檻攔截】如果總價小於門檻，直接回傳 0，商品維持乾淨狀態
        if (totalAmount.compareTo(minAmount) < 0) return BigDecimal.ZERO;

        // 計算折扣比例
        BigDecimal discountRate = BigDecimal.ONE.subtract(new BigDecimal(discount.getDiscountValue().toString())); 
        // 算出總折抵額
        BigDecimal discountAmount = totalAmount.multiply(discountRate);

        // 如果是真算
        if (!isDryRun) {
            // 【規則 4：因為已攤平，直接全數標記】
            markItems(validItems, validItems.size());
            // 【規則 8：退貨分攤】
            apportionDiscount(validItems, discountAmount); 
        }
        // 四捨五入回傳
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    // 【滿額折扣】
    public BigDecimal calculateFixedDiscount(List<CartItemDTO> validItems, Discount discount, boolean isDryRun) {
        // 算總額
        BigDecimal totalAmount = calculateTotalAmount(validItems);
        // 算門檻
        BigDecimal minAmount = getMinAmount(discount);

        // 門檻防護
        if (totalAmount.compareTo(minAmount) < 0) return BigDecimal.ZERO;

        // 取得折抵金
        BigDecimal discountAmount = new BigDecimal(discount.getDiscountValue().toString());
        // 極限保護
        if (discountAmount.compareTo(totalAmount) > 0) discountAmount = totalAmount;

        // 若是真算
        if (!isDryRun) {
            // 全數標記
            markItems(validItems, validItems.size());
            // 執行分攤
            apportionDiscount(validItems, discountAmount); 
        }
        // 四捨五入回傳
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    // 【買 N 送 M】
    public BigDecimal calculateBuyNGetMDiscount(List<CartItemDTO> mainItems, List<CartItemDTO> freeItems, Discount discount, boolean isDryRun) {
        // 取得規則 N
        int buyN = discount.getBuyQuantity();
        // 取得規則 M
        int freeM = discount.getFreeQuantity();
        
        // 算出湊滿幾組 (因為已攤平，直接用 size 代表數量)
        int sets = mainItems.size() / buyN; 
        // 沒湊滿則回傳 0
        if (sets == 0 || freeItems.isEmpty()) return BigDecimal.ZERO;

        // 將贈品依照價格由低到高排序，確保送最便宜的
        freeItems.sort((i1, i2) -> i1.getPrice().compareTo(i2.getPrice()));

        // 算出實際該送的數量
        int actualItemsToFree = Math.min(sets * freeM, freeItems.size());
        
        // 累加贈品價格
        BigDecimal discountAmount = BigDecimal.ZERO;
        // 迴圈跑遍實際要送的贈品
        for (int i = 0; i < actualItemsToFree; i++) {
            // 將該贈品原價加入總折扣
            discountAmount = discountAmount.add(freeItems.get(i).getPrice());
        }

        // 若是真算
        if (!isDryRun) {
            // 【規則 4：餘數釋放】只精準標記「成組」的主商品數量，多餘的維持乾淨！
            List<CartItemDTO> markedMain = markItems(mainItems, sets * buyN);
            // 精準標記被送出的贈品數量
            List<CartItemDTO> markedFree = markItems(freeItems, actualItemsToFree);
            
            // 將真正有參與活動的商品合併成一個清單
            List<CartItemDTO> allBundled = new ArrayList<>(markedMain);
            // 加入贈品
            allBundled.addAll(markedFree);
            // 僅對這群「有被標記」的商品進行退貨分攤！
            apportionDiscount(allBundled, discountAmount); 
        }
        // 回傳折扣
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    // 【條件加購價】
    public BigDecimal calculateAddOnDiscount(List<CartItemDTO> mainItems, List<CartItemDTO> addonItems, Discount discount, boolean isDryRun) {
        // 若主商品件數不足門檻，回傳 0
        if (mainItems.size() < discount.getBuyQuantity() || addonItems.isEmpty()) return BigDecimal.ZERO;

        // 將加購品依價格排序找最便宜的
        addonItems.sort((i1, i2) -> i1.getPrice().compareTo(i2.getPrice()));
        
        // 抓出最便宜加購品的原價
        BigDecimal originalPrice = addonItems.get(0).getPrice(); 
        // 取得活動規定的加購價
        BigDecimal addOnPrice = new BigDecimal(discount.getDiscountValue().toString());
        // 算出省下多少錢
        BigDecimal discountAmount = originalPrice.subtract(addOnPrice);
        
        // 若折扣小於 0 代表沒省到錢，回傳 0
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;

        // 若是真算
        if (!isDryRun) {
            // 只標記剛好達標的主商品數量，釋放餘數
            markItems(mainItems, discount.getBuyQuantity());
            // 只標記 1 件加購品
            List<CartItemDTO> markedAddon = markItems(addonItems, 1);
            // 錢只分攤給這 1 件加購品
            apportionDiscount(markedAddon, discountAmount); 
        }
        // 四捨五入回傳
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    // 【組合價】
    public BigDecimal calculateBundleDiscount(List<CartItemDTO> validItems, Discount discount, boolean isDryRun) {
        // 取得任選件數門檻
        int requiredQty = discount.getBuyQuantity();
        // 算出湊滿幾組
        int sets = validItems.size() / requiredQty;
        
        // 未達標回傳 0
        if (sets == 0) return BigDecimal.ZERO;

        // 依價格由低到高排序，保護老闆利潤
        validItems.sort((i1, i2) -> i1.getPrice().compareTo(i2.getPrice()));

        // 算出參與組合的商品原價總和
        BigDecimal originalTotal = BigDecimal.ZERO;
        // 只跑湊滿組數的件數
        for (int i = 0; i < sets * requiredQty; i++) {
            // 累加原價
            originalTotal = originalTotal.add(validItems.get(i).getPrice());
        }

        // 算出優惠組合總價 (例如 2 組 × 500 = 1000)
        BigDecimal bundleTotal = new BigDecimal(discount.getDiscountValue().toString()).multiply(new BigDecimal(sets));
        // 折扣額 = 原價 - 組合價
        BigDecimal discountAmount = originalTotal.subtract(bundleTotal);

        // 防呆保護
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;

        // 若是真算
        if (!isDryRun) {
            // 精準標記被挑走的商品，多餘的餘數自動釋放！
            List<CartItemDTO> markedItems = markItems(validItems, sets * requiredQty);
            // 對被挑走的商品執行分攤
            apportionDiscount(markedItems, discountAmount); 
        }
        // 回傳金額
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    // ==========================================
    // ⬇️ 輔助工具方法 ⬇️
    // ==========================================

    // 【精準標記方法】只標記指定的數量，並回傳被標記的物件清單以供分攤使用
    private List<CartItemDTO> markItems(List<CartItemDTO> items, int targetCount) {
        // 準備一個用來裝「被標記商品」的空清單
        List<CartItemDTO> markedList = new ArrayList<>();
        // 跑迴圈依序取出需要標記的數量
        for (int i = 0; i < targetCount; i++) {
            // 取得該商品
            CartItemDTO item = items.get(i);
            // 將其狀態改為已使用
            item.setProcessed(true);
            // 加入已標記清單中
            markedList.add(item);
        }
        // 回傳這些已經被綁定的商品
        return markedList;
    }

    // 【規則 8：退貨分攤邏輯】
    private void apportionDiscount(List<CartItemDTO> markedItems, BigDecimal totalDiscount) {
        // 防呆
        if (totalDiscount.compareTo(BigDecimal.ZERO) <= 0) return;
        // 算出這群標記商品的原價分母
        BigDecimal originalTotal = calculateTotalAmount(markedItems);
        // 防呆避免除以零
        if (originalTotal.compareTo(BigDecimal.ZERO) == 0) return;

        // 記錄已經分攤的累計金額
        BigDecimal accumulated = BigDecimal.ZERO;
        
        // 跑迴圈分攤給每一個被標記的獨立商品 (數量皆為 1)
        for (int i = 0; i < markedItems.size(); i++) {
            // 取得目前商品
            CartItemDTO item = markedItems.get(i);
            // 準備宣告分攤額
            BigDecimal itemShare;
            
            // 如果是最後一個商品
            if (i == markedItems.size() - 1) {
                // 用減法吃掉所有的除不盡誤差
                itemShare = totalDiscount.subtract(accumulated);
            } else {
                // 按照原價佔比計算，並四捨五入到整數
                itemShare = totalDiscount.multiply(item.getPrice()).divide(originalTotal, 0, RoundingMode.HALF_UP);
            }
            
            // 實際寫入分攤金額 (若 DTO 有此屬性請解開註解)
            // item.setApportionedAmount(itemShare); 
            
            // 將這筆分攤額累加進紀錄器中
            accumulated = accumulated.add(itemShare);
        }
    }

    // 輔助：計算清單內所有商品原價總和 (因為攤平了，不用再乘以數量)
    private BigDecimal calculateTotalAmount(List<CartItemDTO> items) {
        // 初始化總和 0
        BigDecimal total = BigDecimal.ZERO;
        // 跑迴圈
        for (CartItemDTO item : items) {
            // 直接累加單價
            total = total.add(item.getPrice());
        }
        // 回傳總和
        return total;
    }
    
    // 輔助：安全取得最低門檻
    private BigDecimal getMinAmount(Discount discount) { 
        // 判斷是否為 null，若是則回傳 0
        return discount.getMinimumPurchaseAmount() != null ? discount.getMinimumPurchaseAmount() : BigDecimal.ZERO; 
    }
}