package com.petlife.repository;


import java.math.BigDecimal;
import java.util.List;

//--- 活動新增開始 ---
/**
* 購物車折扣計算後的完整回應物件
*/
public class CartCalculateResponseDTO {
 private BigDecimal discountAmount;
 private BigDecimal finalAmount;
 private BigDecimal originalTotal;
 private List<DiscountDetailDTO> appliedDiscounts;
 private List<CartItemDTO> cartItems;

//💡 新增 originalTotal 的 Getter / Setter
 public BigDecimal getOriginalTotal() { return originalTotal; }
 public void setOriginalTotal(BigDecimal originalTotal) { this.originalTotal = originalTotal; }

 public BigDecimal getDiscountAmount() { return discountAmount; }
 public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

 // 💡 新增 finalAmount 的 Getter / Setter
 public BigDecimal getFinalAmount() { return finalAmount; }
 public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }

 public List<DiscountDetailDTO> getAppliedDiscounts() { return appliedDiscounts; }
 public void setAppliedDiscounts(List<DiscountDetailDTO> appliedDiscounts) { this.appliedDiscounts = appliedDiscounts; }

 public List<CartItemDTO> getCartItems() { return cartItems; }
 public void setCartItems(List<CartItemDTO> cartItems) { this.cartItems = cartItems; }
 
 
 
}