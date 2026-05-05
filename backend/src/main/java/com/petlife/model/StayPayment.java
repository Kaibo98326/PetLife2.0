package com.petlife.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "StayPayment")
@Getter
@Setter
@NoArgsConstructor
public class StayPayment {
	
	@Id@Column(name = "payment_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer paymentId;
	
	@ManyToOne
	@JoinColumn(name = "stay_id")
	private Stay stay;
	
	@Column(name = "payment_method")
	private String paymentMethod;
	
	@Column(name = "payment_status")
	private String paymentStatus;
	
	@Column(name = "amount")
	private Double amount;
	
	@Column(name = "merchant_trade_no")
	private String merchantTradeNo;
	
	@Column(name = "rtn_code")
	private String rtnCode;
	
	@Column(name = "rtn_msg")
	private String rtnMsg;
	
	@Column(name = "trade_no")
	private String tradeNo;
	
	@Column(name = "paid_at")
	private LocalDateTime  paidAt;
	
	@Column(name = "created_at")
	private LocalDateTime  createdAt;
	
}
