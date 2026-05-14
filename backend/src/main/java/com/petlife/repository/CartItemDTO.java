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
    // --- 活動新增結束 ---

    private Integer productId;
    private Integer categoryId;  // 判斷分類活動必備
    private BigDecimal price;
    private Integer quantity;
    private boolean isProcessed = false; // 標記是否已被更高級活動處理過

    public CartItemDTO() {}

    // --- 活動新增開始：Getters and Setters ---
    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }
    
    public String getAppliedDiscountText() { return appliedDiscountText; }
    public void setAppliedDiscountText(String appliedDiscountText) { this.appliedDiscountText = appliedDiscountText; }
    
    public String getReminderText() { return reminderText; }
    public void setReminderText(String reminderText) { this.reminderText = reminderText; }
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
}