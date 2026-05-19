package com.petlife.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "DiscountProduct")
@Getter
@Setter
public class DiscountProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discount_product_id")
    private Integer discountProductId;

    // 引用 Discount 類別
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id")
    private Discount discount;

    //引用組員建立的 Product 類別 
    // 如果專案裡沒有 Product.java，這裡會直接出現紅字報錯
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_role")
    private String productRole = "Main";

    public DiscountProduct() {}

    // 建構子：讀取組員提供的 Product 物件，並綁定到你的活動上
    public DiscountProduct(Discount discount, Product product, String productRole) {
        this.discount = discount;
        this.product = product;
        this.productRole = productRole;
    }
}