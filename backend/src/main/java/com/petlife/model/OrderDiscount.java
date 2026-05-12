package com.petlife.model;



//將 javax 改成 jakarta，以符合新版 Spring Boot 的規範
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "OrderDiscount")
@IdClass(OrderDiscountId.class) // 指定複合主鍵類別
public class OrderDiscount {

 @Id
 @Column(name = "order_id", nullable = false)
 private Integer orderId;

 @Id
 @Column(name = "product_id", nullable = false)
 private Integer productId;

 @Id
 @Column(name = "discount_id", nullable = false)
 private Integer discountId;

 @Column(name = "quantity")
 private Integer quantity;

 @Column(name = "discount_amount", precision = 10, scale = 2)
 private BigDecimal discountAmount;

 // Getters and Setters
 public Integer getOrderId() { return orderId; }
 public void setOrderId(Integer orderId) { this.orderId = orderId; }

 public Integer getProductId() { return productId; }
 public void setProductId(Integer productId) { this.productId = productId; }

 public Integer getDiscountId() { return discountId; }
 public void setDiscountId(Integer discountId) { this.discountId = discountId; }

 public Integer getQuantity() { return quantity; }
 public void setQuantity(Integer quantity) { this.quantity = quantity; }

 public BigDecimal getDiscountAmount() { return discountAmount; }
 public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
}