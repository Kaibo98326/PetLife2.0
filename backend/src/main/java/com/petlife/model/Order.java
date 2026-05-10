package com.petlife.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "[Order]")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;
    
    @Column(name = "member_id", nullable = false)
    private Integer memberId;
    
    @Column(name = "order_name")
    private String orderName;

    @Column(name = "order_phone")
    private String orderPhone;

    @Column(name = "order_date") 
    private LocalDateTime orderDate;

    @Column(name = "order_total")
    private BigDecimal orderTotal;

    @Column(name = "order_status") 
    private String orderStatus;

    @Column(name = "order_note") 
    private String orderNote;

    @Column(name = "order_address", nullable = false)
    private String orderAddress;

    @Column(name = "order_payment", nullable = false)
    private String orderPayment;

    // Vue版本新增: 本次使用點數
    @Column(name = "used_point", nullable = false)
    private Integer usedPoint = 0;

    // Vue版本新增: 使用後剩餘點數
    @Column(name = "remaining_point", nullable = false)
    private Integer remainingPoint = 0;

    @OneToMany(mappedBy = "orderBean", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonManagedReference
    private List<OrderDetail> details = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.orderDate == null) this.orderDate = LocalDateTime.now();
        if (this.orderStatus == null) this.orderStatus = "處理中";
        if (this.orderAddress == null) this.orderAddress = "未填寫";
        if (this.orderPayment == null) this.orderPayment = "未指定";
        
        // 確保存入資料庫時，預設一定是 false (未刪除)
        if (this.isDeleted == null) this.isDeleted = false;
    }
    
    //軟刪除要用的
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}
