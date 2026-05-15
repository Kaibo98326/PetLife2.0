package com.petlife.repository;

import java.math.BigDecimal;

//--- 活動新增開始 ---
/**
* 紀錄單筆成功套用的活動折扣明細 (支援長短名稱分流顯示)
*/
public class DiscountDetailDTO {
 private String name;         // 短名稱 (供結帳頁使用，如：狗狗萬歲)
 private String detailText;   // 完整樣板字串 (供購物車使用，如：狗狗萬歲 滿 $1,000 享 89 折)
 private BigDecimal amount;   // 折抵金額

 public DiscountDetailDTO() {}

 public DiscountDetailDTO(String name, String detailText, BigDecimal amount) {
     this.name = name;
     this.detailText = detailText;
     this.amount = amount;
 }

 public String getName() { return name; }
 public void setName(String name) { this.name = name; }

 public String getDetailText() { return detailText; }
 public void setDetailText(String detailText) { this.detailText = detailText; }

 public BigDecimal getAmount() { return amount; }
 public void setAmount(BigDecimal amount) { this.amount = amount; }
}