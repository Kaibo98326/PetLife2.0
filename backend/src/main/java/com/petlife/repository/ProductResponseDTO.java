package com.petlife.repository;

import java.math.BigDecimal;

/**
 * 業界標準：Data Transfer Object (DTO)
 * 專門用於 API 回傳，避免直接暴露資料庫 Entity 結構
 */
public class ProductResponseDTO {
    // --- 商品基本資訊 ---
    private Integer productId;
    private String productName;
    private BigDecimal productPrice;
    private Integer productStock;
    private String productImage;
    private String categoryName;
    private Integer categoryId;

    // --- 優惠活動標籤 (新增欄位) ---
    private boolean hasActiveDiscount = false; // 是否有活動 (前端 v-if 依據)
    private String activeDiscountName;        // 活動標籤文字 (例如：聖誕85折)
    private String discountType;              // 活動類型 ID (可選，供前端判斷圖示)

    // 空建構子 (JSON 序列化需要)
    public ProductResponseDTO() {}

    // Getters and Setters
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public BigDecimal getProductPrice() { return productPrice; }
    public void setProductPrice(BigDecimal productPrice) { this.productPrice = productPrice; }

    public Integer getProductStock() { return productStock; }
    public void setProductStock(Integer productStock) { this.productStock = productStock; }

    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public boolean isHasActiveDiscount() { return hasActiveDiscount; }
    public void setHasActiveDiscount(boolean hasActiveDiscount) { this.hasActiveDiscount = hasActiveDiscount; }

    public String getActiveDiscountName() { return activeDiscountName; }
    public void setActiveDiscountName(String activeDiscountName) { this.activeDiscountName = activeDiscountName; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
}