package com.petlife.repository;

import java.math.BigDecimal;
/**
 * 用於折扣計算的購物車項目 DTO ------>活動新增
 */
public class CartItemDTO {
    // --- 活動新增開始 ---
    private Integer itemId; // 購物車明細 PK，作為前後端綁定標籤的橋樑
    private String appliedDiscountText; // 存放已折抵的綠色標籤字串 (如: 8折)
    private String reminderText; // 存放未達標的紅色提醒字串 (如: 還差$200...)
    
    // ✨ 修改：擴充前端 Vue 進行智慧分流所需的三個關鍵屬性
    private String discountType; // 活動類型 (如: "4")
    private String productRole;  // 商品角色 ("Main" 或 "Addon")
    private Boolean isActivityMet; // 達標狀態 (true/false)
    // --- 活動新增結束 ---

    private Integer productId;
    private Integer categoryId;  // 判斷分類活動必備
    private BigDecimal price;
    private Integer quantity;
    private boolean isProcessed = false; // 標記是否已被更高級活動處理過
    //kkb新增
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private Integer discountId;

    public Integer getDiscountId() {
        return discountId;
    }

    public void setDiscountId(Integer discountId) {
        this.discountId = discountId;
    }
    //------
    
    public CartItemDTO() {}
    
    // --- 活動新增開始：Getters and Setters ---
    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }
    
    public String getAppliedDiscountText() { return appliedDiscountText; }
    public void setAppliedDiscountText(String appliedDiscountText) { this.appliedDiscountText = appliedDiscountText; }
    
    public String getReminderText() { return reminderText; }
    public void setReminderText(String reminderText) { this.reminderText = reminderText; }
    
    // ✨ 修改：新增三個屬性的 Getter 與 Setter
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    public String getProductRole() { return productRole; }
    public void setProductRole(String productRole) { this.productRole = productRole; }
    public Boolean getIsActivityMet() { return isActivityMet; }
    public void setIsActivityMet(Boolean isActivityMet) { this.isActivityMet = isActivityMet; }
    // --- 活動新增結束 ---

    // 原本的 Getters and Setters
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public boolean isProcessed() { return isProcessed; }
    public void setProcessed(boolean processed) { this.isProcessed = processed; }
    //kkb新增
    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }
}