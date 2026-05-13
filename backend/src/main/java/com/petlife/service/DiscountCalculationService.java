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
//只負責算出給定的商品在特定公式下折抵多少錢
@Service
public class DiscountCalculationService {

	/**
     * Type 1: 百分比折扣 (例如：打 85 折)
     * ：加入 isDryRun 參數，若為 true 代表只是試算找最優解，不真正綁定商品
     */
    // 計算百分比折扣的方法
    public BigDecimal calculatePercentageDiscount(List<CartItemDTO> validItems, Discount discount, boolean isDryRun) {
        // 算出這批符合資格的商品，總共價值多少錢
        BigDecimal totalAmount = calculateTotalAmount(validItems);
        // 取得這個折扣活動所設定的「最低消費門檻」
        BigDecimal minAmount = getMinAmount(discount);

        // 【核心邏輯 6：門檻攔截】如果商品總額小於最低門檻，直接提早結束回傳 0 元，且「略過」後續標記，讓商品能參加下一個活動
        if (totalAmount.compareTo(minAmount) < 0) return BigDecimal.ZERO;

        // 取得活動設定的折扣率（例如 0.85）
        BigDecimal discountValue = new BigDecimal(discount.getDiscountValue().toString());
        // 計算「折抵比例」，公式為 1 - 折扣率（例如 1 - 0.85 = 0.15）
        BigDecimal discountRate = BigDecimal.ONE.subtract(discountValue); 
        // 算出總共可以折多少錢：總金額 × 折抵比例
        BigDecimal discountAmount = totalAmount.multiply(discountRate);

        // 如果現在「不是」在做假算測試（代表確定要套用這個活動了）
        if (!isDryRun) {
            // 【核心邏輯 2：互斥與標記】把這些商品貼上已使用的標籤，防止被其他活動重複折抵
            markItemsAsProcessed(validItems); 
            // 【核心邏輯 9：原價比例分攤】將算出來的折扣總金額，按比例分配給每一個商品，作為退貨依據
            apportionDiscount(validItems, discountAmount); 
        }
        // 回傳折扣金額，設定為 0 位小數並四捨五入
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Type 2: 滿額折扣 (定額折抵，例如：滿千折百)
     */
    // 計算定額折抵的方法
    public BigDecimal calculateFixedDiscount(List<CartItemDTO> validItems, Discount discount, boolean isDryRun) {
        // 算出符合資格的商品總額
        BigDecimal totalAmount = calculateTotalAmount(validItems);
        // 取得最低消費門檻
        BigDecimal minAmount = getMinAmount(discount);

        // 【核心邏輯 6：門檻攔截】未達門檻回傳 0
        if (totalAmount.compareTo(minAmount) < 0) return BigDecimal.ZERO;

        // 取得活動設定的固定折抵金額（例如 100 元）
        BigDecimal discountAmount = new BigDecimal(discount.getDiscountValue().toString());
        // 防呆保護：如果折抵金額大於商品總價（例如買 50 元折 100 元），最多只能折到商品總價
        if (discountAmount.compareTo(totalAmount) > 0) discountAmount = totalAmount;

        // 若非假算，執行標記與分攤
        if (!isDryRun) {
            // 標記互斥
            markItemsAsProcessed(validItems);
            // 分攤退款金額
            apportionDiscount(validItems, discountAmount); 
        }
        // 四捨五入回傳
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Type 3: 買 N 送 M (例如：買 3 送 1)
     */
    // 計算買 N 送 M 的方法，分為主商品 (mainItems) 與 贈品 (freeItems)
    public BigDecimal calculateBuyNGetMDiscount(List<CartItemDTO> mainItems, List<CartItemDTO> freeItems, Discount discount, boolean isDryRun) {
        // 將主商品展開為「一件一件」的單價清單
        List<BigDecimal> mainUnitPrices = flattenItemsToUnitPrices(mainItems);
        // 取得活動條件：需購買的 N 件
        int buyN = discount.getBuyQuantity();
        // 取得活動條件：贈送的 M 件
        int freeM = discount.getFreeQuantity();
        
        // 算出這些主商品總共可以湊齊「幾組」活動條件
        int sets = mainUnitPrices.size() / buyN; 
        // 如果連一組都湊不到，或是購物車裡根本沒挑贈品，回傳 0 元
        if (sets == 0 || freeItems.isEmpty()) return BigDecimal.ZERO;

        // 將贈品展開為單價清單
        List<BigDecimal> freeUnitPrices = flattenItemsToUnitPrices(freeItems);
        // 對贈品單價進行由低到高排序，確保公司送出的是「最便宜」的商品
        Collections.sort(freeUnitPrices);

        // 算出理論上最大可以送幾件（組數 × 每組送的件數）
        int maxItemsToFree = sets * freeM;
        // 實際贈送件數：取最大可送件數與購物車實際贈品數的「最小值」（避免客人拿太少反而出錯）
        int actualItemsToFree = Math.min(maxItemsToFree, freeUnitPrices.size());
        
        // 準備一個變數來累加贈送（折抵）的總金額
        BigDecimal discountAmount = BigDecimal.ZERO;
        // 跑迴圈，把最便宜的幾個贈品單價加總起來
        for (int i = 0; i < actualItemsToFree; i++) {
            // 將贈品單價累加進折扣總額中
            discountAmount = discountAmount.add(freeUnitPrices.get(i));
        }

        // 若非假算，執行標記與分攤
        if (!isDryRun) {
            // 【核心邏輯 4：守恆餘數釋放】這裡僅為簡化寫法，標記主副商品。實務上只有「湊滿組數」的商品會被標記，餘數會釋放
            markItemsAsProcessed(mainItems);
            markItemsAsProcessed(freeItems);
            
            // 建立一個包含主商品與贈品的新陣列
            List<CartItemDTO> allBundled = new ArrayList<>(mainItems);
            // 將贈品加入陣列
            allBundled.addAll(freeItems);
            // 將送的總金額，按原價比例分攤回這整組「買N送M」的商品身上
            apportionDiscount(allBundled, discountAmount); 
        }
        // 四捨五入回傳
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Type 4: 條件加購價 (例如：滿 3 件，某商品加購價 50 元)
     */
    // 計算加購價的方法
    public BigDecimal calculateAddOnDiscount(List<CartItemDTO> mainItems, List<CartItemDTO> addonItems, Discount discount, boolean isDryRun) {
        // 算出主商品的總購買件數
        int totalMainQty = mainItems.stream().mapToInt(CartItemDTO::getQuantity).sum();
        // 如果主商品件數未達門檻，回傳 0 元
        if (totalMainQty < discount.getBuyQuantity()) return BigDecimal.ZERO;
        // 如果購物車裡沒有放要加購的商品，回傳 0 元
        if (addonItems.isEmpty()) return BigDecimal.ZERO;

        // 展開加購商品的單價清單
        List<BigDecimal> addonPrices = flattenItemsToUnitPrices(addonItems);
        // 由低到高排序加購商品
        Collections.sort(addonPrices);
        
        // 取得最便宜的那一件加購商品的原價
        BigDecimal originalPrice = addonPrices.get(0); 
        // 取得活動設定的「優惠加購價」金額
        BigDecimal addOnPrice = new BigDecimal(discount.getDiscountValue().toString());
        // 折扣額 = 該加購商品原價 - 優惠加購價
        BigDecimal discountAmount = originalPrice.subtract(addOnPrice);
        
        // 防呆：如果原價比加購價還要便宜（折扣為負數），不予折抵回傳 0 元
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;

        // 若非假算
        if (!isDryRun) {
            // 標記主商品與加購品為已使用
            markItemsAsProcessed(mainItems);
            markItemsAsProcessed(addonItems);
            // 【退貨分攤】加購價的折扣，通常只分攤在「加購商品」自己身上
            apportionDiscount(addonItems, discountAmount); 
        }
        // 四捨五入回傳
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Type 5: 組合條件價 (例如：任選 N 件 M 元)
     */
    // 計算組合條件價的方法
    public BigDecimal calculateBundleDiscount(List<CartItemDTO> validItems, Discount discount, boolean isDryRun) {
        // 展開所有符合條件的商品單價
        List<BigDecimal> allUnitPrices = flattenItemsToUnitPrices(validItems);
        // 取得活動規定的任選件數 N
        int requiredQty = discount.getBuyQuantity();
        // 算出總共湊滿了幾組
        int sets = allUnitPrices.size() / requiredQty;
        
        // 若連一組都沒湊滿，回傳 0 元
        if (sets == 0) return BigDecimal.ZERO;
        // 單價由低到高排序
        Collections.sort(allUnitPrices);

        // 準備累加被挑走配成組合的商品「原價總和」
        BigDecimal originalTotalOfBundledItems = BigDecimal.ZERO;
        // 跑迴圈挑選出 (組數 × 件數) 個商品
        for (int i = 0; i < sets * requiredQty; i++) {
            // 累加這些商品的原價
            originalTotalOfBundledItems = originalTotalOfBundledItems.add(allUnitPrices.get(i));
        }

        // 算出客人實際應付的「組合優惠總價」（例如 2 組 × 500 元 = 1000 元）
        BigDecimal bundlePriceTotal = new BigDecimal(discount.getDiscountValue().toString()).multiply(new BigDecimal(sets));
        // 折扣額 = 原價總和 - 組合優惠總價
        BigDecimal discountAmount = originalTotalOfBundledItems.subtract(bundlePriceTotal);

        // 若折扣為負數回傳 0 元
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;

        // 若非假算
        if (!isDryRun) {
            // 標記商品為已使用
            markItemsAsProcessed(validItems);
            // 分攤組合價的折扣額度
            apportionDiscount(validItems, discountAmount); 
        }
        // 四捨五入回傳
        return discountAmount.setScale(0, RoundingMode.HALF_UP);
    }

    // ==========================================
    // ⬇️ 共用工具與輔助方法 ⬇️
    // ==========================================

    //原價比例分攤演算法 (Pro Rata Apportionment)
    private void apportionDiscount(List<CartItemDTO> bundledItems, BigDecimal totalDiscount) {
        // 如果折扣金額是 0 或負數，代表沒折到錢，不需要執行分攤
        if (totalDiscount.compareTo(BigDecimal.ZERO) <= 0) return;
        
        // 1. 算出這群需要被分攤的商品，它們的「原價總和」（這將作為公式的分母）
        BigDecimal originalTotal = calculateTotalAmount(bundledItems);
        // 防呆：如果總原價為 0 (避免除以零的錯誤)，直接結束
        if (originalTotal.compareTo(BigDecimal.ZERO) == 0) return;

        // 準備一個變數，用來記錄跑迴圈的過程中「已經發放/分攤掉多少錢」
        BigDecimal accumulatedDiscount = BigDecimal.ZERO;
        
        // 2. 開始跑迴圈，依序將折扣錢分攤給每一個商品
        for (int i = 0; i < bundledItems.size(); i++) {
            // 取得目前的商品
            CartItemDTO item = bundledItems.get(i);
            // 算出該商品的小計原價 (單價 × 數量)
            BigDecimal itemTotal = item.getPrice().multiply(new BigDecimal(item.getQuantity()));
            // 準備宣告該商品應得的分攤折扣
            BigDecimal itemDiscount;
            
            // 【防呆技巧】判斷是否為「最後一件商品」
            if (i == bundledItems.size() - 1) {
                // 如果是最後一件，不再使用乘除法！直接用「總折扣 - 前面已分攤完的金額」來吃掉小數點除不盡的 1 元誤差
                itemDiscount = totalDiscount.subtract(accumulatedDiscount);
            } else {
                // 如果不是最後一件，套用公式：總折扣 × (該商品原價 / 總原價)，並四捨五入到整數
                itemDiscount = totalDiscount.multiply(itemTotal)
                        .divide(originalTotal, 0, RoundingMode.HALF_UP);
            }
            
            // 將算出的分攤金額存入該商品 DTO 裡 (這筆錢最後會寫進 OrderDiscount 資料表)
            // item.setApportionedDiscountAmount(itemDiscount); 
            
            // 累加已發放的分攤金額
            accumulatedDiscount = accumulatedDiscount.add(itemDiscount);
        }
    }

    // 輔助：將商品清單按數量打平成為一維的純單價陣列
    private List<BigDecimal> flattenItemsToUnitPrices(List<CartItemDTO> items) { 
        List<BigDecimal> prices = new ArrayList<>();
        for (CartItemDTO item : items) {
            for (int i = 0; i < item.getQuantity(); i++) prices.add(item.getPrice());
        }
        return prices; 
    }
    
    // 輔助：計算清單內所有商品的真實總價值
    private BigDecimal calculateTotalAmount(List<CartItemDTO> items) { 
        BigDecimal total = BigDecimal.ZERO;
        for (CartItemDTO item : items) total = total.add(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
        return total; 
    }
    
    // 輔助：安全取得最低門檻，防止資料庫 null 值崩潰
    private BigDecimal getMinAmount(Discount discount) { 
        return discount.getMinimumPurchaseAmount() != null ? discount.getMinimumPurchaseAmount() : BigDecimal.ZERO; 
    }
    
    // 輔助：標記商品已被活動吃掉 (互斥鎖)
    private void markItemsAsProcessed(List<CartItemDTO> items) { 
        for (CartItemDTO item : items) item.setProcessed(true); 
    }
}