package com.petlife.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "OrderDiscount")
@Getter
@Setter
public class OrderDiscount {

    // 建議新增的流水號主鍵，滿足 JPA 規範
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_discount_id")
    private Integer orderDiscountId;

    // 關聯到 Order (訂單) 實體 (假設你有 Order 這個類別)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 關聯到 Product (商品) 實體 (假設你有 Product 這個類別)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 關聯到我們剛剛建立的 Discount (折扣) 實體
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id", nullable = false)
    private Discount discount;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    //無參數建構子 (JPA 框架必備)
    public OrderDiscount() {
    }

    // 帶參數建構子 
    // 用來讀取使用者的參數，並映射到物件的屬性上
    public OrderDiscount(Order order, Product product, Discount discount, Integer quantity, BigDecimal discountAmount) {
        this.order = order;
        this.product = product;
        this.discount = discount;
        this.quantity = quantity;
        this.discountAmount = discountAmount;
    }
}