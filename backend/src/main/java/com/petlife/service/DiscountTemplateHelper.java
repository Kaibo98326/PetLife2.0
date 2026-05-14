package com.petlife.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import com.petlife.model.Discount;
//--- 活動新增開始 ---
/**
* 負責將折扣數字翻譯成前端所需的行銷字串 (職責分離)
*/
@Component
public class DiscountTemplateHelper {

 // 1. 生成已折抵的綠色標籤
 public String generateAppliedText(Discount discount) {
     int type = discount.getDiscountType().getDiscountTypeId();
     if (type == 1) {
    	 int val = discount.getDiscountValue().multiply(BigDecimal.valueOf(100)).intValue(); 
         return (val % 10 == 0 ? (val / 10) : val) + "折";
     } else if (type == 2) {
         return "現折 " + discount.getDiscountValue().intValue();
     } else if (type == 3) {
         return "買" + discount.getBuyQuantity() + "送" + discount.getFreeQuantity();
     } else if (type == 4) {
         return "加購只要 $" + discount.getDiscountValue().intValue();
     } else if (type == 5) {
         return "任選" + discount.getBuyQuantity() + "件 $" + discount.getDiscountValue().intValue() + "元";
     }
     return "";
 }

 // 2. 生成未達標的紅色提醒
 public String generateReminderText(Discount discount, BigDecimal diffAmt, int diffQty) {
     String name = discount.getDiscountName();
     int type = discount.getDiscountType().getDiscountTypeId();
     
     if (type == 1) {
         int val = discount.getDiscountValue().multiply(BigDecimal.valueOf(100)).intValue();
         String discountStr = (val % 10 == 0 ? (val / 10) : val) + "折";
         return name + "：還差 <b>$" + diffAmt.intValue() + "</b> 享 <b>" + discountStr + "</b>";
     } else if (type == 2) {
         return name + "：還差 <b>$" + diffAmt.intValue() + "</b> 現折 <b>" + discount.getDiscountValue().intValue() + "</b>";
     } else if (type == 3) {
         return name + "：還差 <b>" + diffQty + "件</b> 享 <b>買" + discount.getBuyQuantity() + "送" + discount.getFreeQuantity() + "</b>";
     } else if (type == 4) {
         return name + "：還差 <b>" + diffQty + "件</b> 享加購 <b>$" + discount.getDiscountValue().intValue() + "</b>";
     } else if (type == 5) {
         return name + "：還差 <b>" + diffQty + "件</b> 任選" + discount.getBuyQuantity() + "件 <b>$" + discount.getDiscountValue().intValue() + "</b>";
     }
     return "";
 }

 // 3. 生成購物車展開明細用的完整樣板字串
 public String generateDiscountDetailText(Discount discount) {
     String name = discount.getDiscountName();
     int type = discount.getDiscountType().getDiscountTypeId();
     
     if (type == 1) {
         int val = discount.getDiscountValue().multiply(BigDecimal.valueOf(100)).intValue();
         String discountStr = (val % 10 == 0 ? (val / 10) : val) + "折";
         String minStr = (discount.getMinimumPurchaseAmount() != null && discount.getMinimumPurchaseAmount().compareTo(BigDecimal.ZERO) > 0) ? " 滿 $" + discount.getMinimumPurchaseAmount().intValue() : "";
         return name + minStr + " 享 " + discountStr;
     } else if (type == 2) {
         String minStr = (discount.getMinimumPurchaseAmount() != null && discount.getMinimumPurchaseAmount().compareTo(BigDecimal.ZERO) > 0) ? " 滿 $" + discount.getMinimumPurchaseAmount().intValue() : "";
         return name + minStr + " 現折 " + discount.getDiscountValue().intValue();
     } else if (type == 3) {
         return name + " 買" + discount.getBuyQuantity() + "件送" + discount.getFreeQuantity() + "件";
     } else if (type == 4) {
         return name + " 買" + discount.getBuyQuantity() + "件, 加購只要 $" + discount.getDiscountValue().intValue();
     } else if (type == 5) {
         return name + " 任選" + discount.getBuyQuantity() + "件 $" + discount.getDiscountValue().intValue() + "元";
     }
     return name;
 }
}
