package com.petlife.repository;

import java.math.BigDecimal;

// 活動修改：新增 DTO，用於回傳訂單折扣摘要（活動名稱 + 折抵金額），供結帳成功頁顯示使用
public class OrderDiscountSummaryDTO {

    private String name;
    private BigDecimal amount;

    public OrderDiscountSummaryDTO() {}

    public OrderDiscountSummaryDTO(String name, BigDecimal amount) {
        this.name = name;
        this.amount = amount;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
