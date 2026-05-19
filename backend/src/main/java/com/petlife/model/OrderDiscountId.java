package com.petlife.model;

import java.io.Serializable;
import java.util.Objects;

// 複合主鍵類別
public class OrderDiscountId implements Serializable {
    
    private Integer orderId;
    private Integer productId;
    private Integer discountId;

    public OrderDiscountId() {}

    public OrderDiscountId(Integer orderId, Integer productId, Integer discountId) {
        this.orderId = orderId;
        this.productId = productId;
        this.discountId = discountId;
    }

    // 必須實作 equals 和 hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderDiscountId that = (OrderDiscountId) o;
        return Objects.equals(orderId, that.orderId) &&
               Objects.equals(productId, that.productId) &&
               Objects.equals(discountId, that.discountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, productId, discountId);
    }

    // Getters and Setters
    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }
    public Integer getDiscountId() { return discountId; }
    public void setDiscountId(Integer discountId) { this.discountId = discountId; }
}