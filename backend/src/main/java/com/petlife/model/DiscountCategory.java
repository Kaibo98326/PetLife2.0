package com.petlife.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "DiscountCategory")
@Getter
@Setter
public class DiscountCategory {

    //新增流水號主鍵
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discount_category_id")
    private Integer discountCategoryId;

    // 關聯到 Discount (折扣活動)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id", nullable = false)
    private Discount discount;

    // 關聯到 Category (商品分類) - Category實體類別
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // 無參數建構子 (JPA 框架必備) 
    public DiscountCategory() {
    }

    // 帶參數建構子
    // 日後綁定分類時，只需 new DiscountCategory(活動物件, 分類物件);
    public DiscountCategory(Discount discount, Category category) {
        this.discount = discount;
        this.category = category;
    }
}