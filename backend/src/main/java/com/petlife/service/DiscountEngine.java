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

	// 讓 Spring 自動幫我們注入「折扣計算服務」這個會計小幫手
    @Autowired
    private DiscountCalculationService calculationService;

    // 執行整台購物車折扣計算的「核心入口點」
    public BigDecimal executeDiscount(List<CartItemDTO> cartItems, List<Discount> allActiveDiscounts) {
        // 初始化本次結帳總共幫客人省下多少錢的變數為 0
        BigDecimal totalSaved = BigDecimal.ZERO;

        // 1. 利用 Stream 把所有活動進行分組：挑出所有 ScopeType 為 2 的「指定單品」活動
        List<Discount> productLevelDiscounts = allActiveDiscounts.stream()
                // 過濾條件：ScopeType 等於 2
                .filter(d -> d.getScopeType() == 2) 
                // 將結果收集成一個 List
                .collect(Collectors.toList());

        // 1. 利用 Stream 把所有活動進行分組：挑出所有 ScopeType 為 1 的「指定分類」活動
        List<Discount> categoryLevelDiscounts = allActiveDiscounts.stream()
                // 過濾條件：ScopeType 等於 1
                .filter(d -> d.getScopeType() == 1) 
                // 將結果收集成一個 List
                .collect(Collectors.toList());

        // --- 第一梯次：優先執行「指定單品」的活動 (業界慣例：單品活動優先權高於全館分類) ---
        // 跑迴圈遍歷所有單品活動
        for (Discount discount : productLevelDiscounts) {
            // 把購物車和該活動丟給「分發中心」算錢，算出該活動折了多少
            BigDecimal saved = dispatchCalculation(cartItems, discount);
            // 把算出來的折扣金額，累加到總省下金額中
            totalSaved = totalSaved.add(saved);
        }

        // --- 第二梯次：執行「指定分類」的活動 (單品沒處理到的商品，由分類活動接手) ---
        // 跑迴圈遍歷所有分類活動
        for (Discount discount : categoryLevelDiscounts) {
            // 丟給分發中心算錢
            BigDecimal saved = dispatchCalculation(cartItems, discount);
            // 累加折扣金額
            totalSaved = totalSaved.add(saved);
        }

        // 所有活動都跑完後，回傳最終總共省下的金額
        return totalSaved;
    }

    // 邏輯分發中心：負責判斷這個活動屬於哪一種類型，並呼叫對應的計算公式
    private BigDecimal dispatchCalculation(List<CartItemDTO> cartItems, Discount discount) {
        // 1. 呼叫底下的篩選器，撈出購物車裡「符合這個活動主商品資格」且「還沒被打折過」的商品
        List<CartItemDTO> eligibleItems = filterEligibleItems(cartItems, discount, "Main");
        
        // 如果購物車裡連一件符合資格的主商品都沒有，直接提早下班回傳 0 元折扣
        if (eligibleItems.isEmpty()) return BigDecimal.ZERO;

        // 2. 取得這個活動的 DiscountType ID (1到5)
        Integer typeId = discount.getDiscountType().getDiscountTypeId();
        
        // 根據 Type ID 進行 Switch 判斷分流
        switch (typeId) {
            // 若為 Type 1: 百分比折扣
            case 1: 
                // 呼叫會計小幫手的百分比折扣演算法
                return calculationService.calculatePercentageDiscount(eligibleItems, discount);
            // 若為 Type 2: 滿額定額折抵
            case 2: 
                // 呼叫會計小幫手的定額折扣演算法
                return calculationService.calculateFixedDiscount(eligibleItems, discount);
            // 若為 Type 3: 買 N 送 M
            case 3: { 
                // 必須額外再呼叫一次篩選器，撈出符合「副商品(贈品)」資格的項目
                List<CartItemDTO> freeItems = filterEligibleItems(cartItems, discount, "Addon");
                // 把主、副商品一起丟給買 N 送 M 演算法
                return calculationService.calculateBuyNGetMDiscount(eligibleItems, freeItems, discount);
            }
            // 若為 Type 4: 條件加購價
            case 4: { 
                // 必須額外再呼叫一次篩選器，撈出符合「副商品(加購)」資格的項目
                List<CartItemDTO> addonItems = filterEligibleItems(cartItems, discount, "Addon");
                // 把主、副商品一起丟給加購價演算法
                return calculationService.calculateAddOnDiscount(eligibleItems, addonItems, discount);
            }
            // 若為 Type 5: 組合條件價
            case 5: 
                // 呼叫會計小幫手的組合條件價演算法
                return calculationService.calculateBundleDiscount(eligibleItems, discount);
            // 若 Type ID 未知 (防呆機制)
            default:
                // 預設不給任何折扣，回傳 0 元
                return BigDecimal.ZERO;
        }
    }

    // 商品篩選器：最核心的過濾機制，決定誰有資格參加活動
    private List<CartItemDTO> filterEligibleItems(List<CartItemDTO> cartItems, Discount discount, String role) {
        // 第一步過濾：把購物車裡「還沒有被標記為 processed」的商品挑出來，確保每個商品只會享受一次最優惠的活動
        List<CartItemDTO> availableItems = cartItems.stream()
                // 判斷 isProcessed() 是否為 false
                .filter(item -> !item.isProcessed())
                // 收集成一個可用的乾淨清單
                .collect(Collectors.toList());

        // 判斷活動範圍，如果 ScopeType 是 1 (指定分類)
        if (discount.getScopeType() == 1) {
            // 從活動設定檔中，把符合角色(主項或副項)的「分類 ID」全部撈出來變成一個 Set 集合
            Set<Integer> targetCatIds = discount.getDiscountCategories().stream()
                    // 檢查資料庫裡的 CategoryRole 是否與傳入的 role (Main/Addon) 相符
                    .filter(dc -> role.equals(dc.getCategoryRole()))
                    // 提取出分類的 CategoryId
                    .map(dc -> dc.getCategory().getCategoryId())
                    // 收集成一個不重複的 Set 集合
                    .collect(Collectors.toSet());
            
            // 從可用的乾淨清單中，比對「商品身上的分類 ID」是否包含在我們的目標集合裡
            return availableItems.stream()
                    // 若 targetCatIds 包含該商品的 CategoryId，代表符合資格留下來
                    .filter(item -> targetCatIds.contains(item.getCategoryId()))
                    // 收集成最終合格清單回傳
                    .collect(Collectors.toList());
                    
        // 如果 ScopeType 不是 1 (代表是 2：指定單品)
        } else {
            // 從活動設定檔中，把符合角色(主項或副項)的「商品 ID」全部撈出來變成一個 Set 集合
            Set<Integer> targetProdIds = discount.getDiscountProducts().stream()
                    // 檢查資料庫裡的 ProductRole 是否與傳入的 role (Main/Addon) 相符
                    .filter(dp -> role.equals(dp.getProductRole()))
                    // 提取出商品的 ProductId
                    .map(dp -> dp.getProduct().getProductId())
                    // 收集成一個不重複的 Set 集合
                    .collect(Collectors.toSet());
            
            // 從可用的乾淨清單中，比對「商品本身的 ID」是否包含在我們的目標集合裡
            return availableItems.stream()
                    // 若 targetProdIds 包含該商品的 ProductId，代表是指定商品留下來
                    .filter(item -> targetProdIds.contains(item.getProductId()))
                    // 收集成最終合格清單回傳
                    .collect(Collectors.toList());
        }
    }
}
