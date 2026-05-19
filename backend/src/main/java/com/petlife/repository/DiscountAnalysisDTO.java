package com.petlife.repository;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 專門用來回傳給 Vue 儀表板圓餅圖的 DTO 報表物件
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountAnalysisDTO {
    private String productName;
    private BigDecimal discountAmount;
    private Integer quantity;
}