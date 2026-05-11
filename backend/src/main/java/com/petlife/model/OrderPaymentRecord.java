package com.petlife.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "OrderPaymentRecord")
@Data
@NoArgsConstructor
public class OrderPaymentRecord implements Serializable {
	private static final long serialVersionUID = 1L;
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_payment_id")
    private Integer orderPaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "merchant_trade_no", nullable = false, length = 50)
    private String merchantTradeNo;

    @Column(name = "payment_type", nullable = false)
    private String paymentType;

    @Column(name = "payment_status")
    private String paymentStatus = "尚未付款";

    @Column(name = "rtn_code")
    private Integer rtnCode;

    @Column(name = "rtn_msg")
    private String rtnMsg;

    @Column(name = "trade_no")
    private String tradeNo;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate = LocalDateTime.now();

    @Column(name = "pay_time")
    private LocalDateTime payTime;

    @Column(name = "payment_amt")
    private BigDecimal paymentAmt;
}
