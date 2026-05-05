package com.petlife.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "DiscountType")
@Getter
@Setter
public class DiscountType {

    @Id
    @Column(name = "discount_type_id")
    private Integer discountTypeId;

    @Column(name = "discount_type_name", length = 100)
    private String discountTypeName;

    @Column(name = "discount_code", length = 50, nullable = false)
    private String discountCode;

    //無參數建構子 (JPA 框架必備)
    public DiscountType() {
    }

    //帶參數建構子 (方便寫程式時產生物件) 
    public DiscountType(Integer discountTypeId, String discountTypeName, String discountCode) {
        this.discountTypeId = discountTypeId;
        this.discountTypeName = discountTypeName;
        this.discountCode = discountCode;
    }

    // 原本這裡一堆的 getDiscountTypeId() 和 setDiscountTypeId() 等方法，
    // 現在全部都可以刪除不寫了，因為頂部的註解已經幫我們處理好了！
}