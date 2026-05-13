package com.petlife.repository;

import java.math.BigDecimal;
import java.util.List;



/*
 * 購物車折扣計算後的完整回應物件
 */
public class CartCalculateResponseDTO {
    private BigDecimal discountAmount;
    private List<DiscountDetailDTO> appliedDiscounts;
    private List<CartItemDTO> cartItems;

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public List<DiscountDetailDTO> getAppliedDiscounts() { return appliedDiscounts; }
    public void setAppliedDiscounts(List<DiscountDetailDTO> appliedDiscounts) { this.appliedDiscounts = appliedDiscounts; }

    public List<CartItemDTO> getCartItems() { return cartItems; }
    public void setCartItems(List<CartItemDTO> cartItems) { this.cartItems = cartItems; }
}
