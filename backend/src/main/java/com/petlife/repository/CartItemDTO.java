package com.petlife.repository;

import java.math.BigDecimal;
  
/**
 * 用於折扣計算的購物車項目 DTO ------>活動新增
 */
public class CartItemDTO {
    private Integer productId;
    private Integer categoryId;  // 判斷分類活動必備
    private BigDecimal price;
    private Integer quantity;
    private boolean isProcessed = false; // 標記是否已被更高級活動處理過

    public CartItemDTO() {}

    // Getters and Setters
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