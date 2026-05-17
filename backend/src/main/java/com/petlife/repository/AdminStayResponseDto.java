package com.petlife.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class AdminStayResponseDto {
	// Stay 基本資料
	private Integer stayId;
	private LocalDate stayStartDate;
	private LocalDate stayEndDate;
	private Integer stayDay;
	private Integer petCount;
	private Double sumPrice;
	private String stayStatus;
	private String stayRemark;
	
	// 會員資訊
	private Integer memberId;
	private String memberName;
	private String memberPhone;
	private String memberEmail;
	
	// 寵物資訊
	private String mainPetName;
	private String mainPetSpecies;
	private String mainPetBreed;
	private List<String> otherPetNames; // 其他寵物名稱
	
	// 房間資訊
	private String roomNo;
	private String roomTypeName;
	
	// 支付資訊
	private String paymentMethod;
	private String paymentStatus;
	
	// 時間戳
	private LocalDateTime createdAt;   // 訂單成立時間
	private LocalDateTime paidAt;      // 付款時間
	private LocalDateTime checkedInAt; // 入住時間
	private LocalDateTime checkedOutAt; // 退房時間
	private LocalDateTime cancelledAt;  // 取消時間
}