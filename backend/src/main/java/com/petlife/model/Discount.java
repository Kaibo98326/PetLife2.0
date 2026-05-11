package com.petlife.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.HashSet;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
/**
 * [折扣活動實體]
 * * 💡 欄位與活動類型 (DiscountType) 對應關係表：
 * --------------------------------------------------------------------------------------------------
 * 活動代碼 (Code)       | discount_value | buy_quantity | free_quantity | minimum_purchase_amount
 * --------------------------------------------------------------------------------------------------
 * 1. PERCENTAGE         | 折扣率 (如 0.8) | -            | -             | 滿額觸發門檻 (通用)
 * 2. AMOUNT_OFF         | 折抵金額        | -            | -             | 滿額觸發門檻 (通用)
 * 3. BUY_N_GET_M        | -              | 買 N 件      | 送 M 件       | 滿額觸發門檻 (通用)
 * 4. CONDITIONAL_ADDON  | 加購價金額      | 主商品需滿 N 件 | -             | 滿額觸發門檻 (通用)
 * 5. BUNDLE_PRICE       | 組合總價        | 任選 N 件     | -             | 滿額觸發門檻 (通用)
 * --------------------------------------------------------------------------------------------------
 * 註："-" 代表該活動不適用該欄位，存檔時建議帶入 NULL。
 */
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
    private String status = "active";

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "discount_description", length = 2000)
    private String discountDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_type_id")
    private DiscountType discountType;

    @Column(name = "scope_type", nullable = false)
    private Byte scopeType; // 1:分類, 2:單品

    /* --- 以下為動態規則欄位 --- */

    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue; // 折扣率、折抵金額、加購價、組合價

    @Column(name = "buy_quantity")
    private Integer buyQuantity; // 買N送M的N、加購/組合的門檻數量

    @Column(name = "free_quantity")
    private Integer freeQuantity; // 買N送M的M (贈送數量)

    @Column(name = "minimum_purchase_amount", precision = 10, scale = 2)
    private BigDecimal minimumPurchaseAmount; // [全活動適用] 觸發活動的最低消費總額
    
 // 使用 Set 可避免 Hibernate 多重 List 抓取的 Exception
    @OneToMany(mappedBy = "discount", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonIgnoreProperties("discount") // 防止 JSON 無限迴圈
    private Set<DiscountCategory> discountCategories = new HashSet<>();

    @OneToMany(mappedBy = "discount", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonIgnoreProperties("discount") // 防止 JSON 無限迴圈
    private Set<DiscountProduct> discountProducts = new HashSet<>();

    // JPA 必備無參數建構子
    public Discount() {
    }

    // 全欄位建構子
    public Discount(String discountName, String status, LocalDate startDate, LocalDate endDate, 
                    String discountDescription, DiscountType discountType, Byte scopeType, 
                    BigDecimal discountValue, Integer buyQuantity, Integer freeQuantity, 
                    BigDecimal minimumPurchaseAmount) {
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