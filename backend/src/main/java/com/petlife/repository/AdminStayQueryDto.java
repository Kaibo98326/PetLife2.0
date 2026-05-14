package com.petlife.repository;

import java.time.LocalDate;
import lombok.Data;

@Data
public class AdminStayQueryDto {
	private Integer page;          // 頁數（0 開始）
	private Integer size;          // 每頁筆數
	private String stayStatus;     // 訂單狀態（待支付、已確認、已入住、已退房、已取消）
	private String memberName;     // 會員名稱
	private String memberPhone;    // 電話末三碼
	private String stayId;         // 訂單編號
	private LocalDate startDate;   // 搜尋開始日期（可選）
	private LocalDate endDate;     // 搜尋結束日期（可選）
}