package com.petlife.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Discount")
@Getter
@Setter
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discount_id")
    private Integer discountId;

    @Column(name = "discount_name", length = 100)
    private String discountName;

    @Column(name = "status", length = 20)
    private String status = "active"; // 預設值

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "discount_description", length = 2000)
    private String discountDescription;

    // 關聯到 DiscountType 實體
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_type_id")
    private DiscountType discountType;

    @Column(name = "scope_type", nullable = false)
    private Byte scopeType;

    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "buy_quantity")
    private Integer buyQuantity;

    @Column(name = "free_quantity")
    private Integer freeQuantity;

    @Column(name = "minimum_purchase_amount", precision = 10, scale = 2)
    private BigDecimal minimumPurchaseAmount;

    // 無參數建構子 (JPA 框架必備)
    // Hibernate 從資料庫撈取資料轉換成 Java 物件時，必須先呼叫這個空的建構子
    public Discount() {
    }

    //帶參數建構子 
    // 注意：這裡不包含 discountId，因為它是 IDENTITY 自動遞增的，由資料庫負責產生
    public Discount(String discountName, String status, LocalDate startDate, LocalDate endDate, 
                    String discountDescription, DiscountType discountType, Byte scopeType, 
                    BigDecimal discountValue, Integer buyQuantity, Integer freeQuantity, 
                    BigDecimal minimumPurchaseAmount) {
        
        // 將傳入的參數指派給物件的屬性
        this.discountName = discountName;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.discountDescription = discountDescription;
        this.discountType = discountType;
        this.scopeType = scopeType;
        this.discountValue = discountValue;
        this.buyQuantity = buyQuantity;
        this.freeQuantity = freeQuantity;
        this.minimumPurchaseAmount = minimumPurchaseAmount;
    }
}
