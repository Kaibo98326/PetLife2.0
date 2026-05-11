package com.petlife.model;

import java.io.Serializable;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Entity 
@Table(name="OrderDetail")
@IdClass(OrderDetailId.class) 
@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class OrderDetail implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id") 
    @ToString.Exclude
    @JsonBackReference
    private Order orderBean; 
    
    @Id
    @Column(name="product_id") 
    private Integer productId;
    
    @Column(name="product_name") @NonNull
    private String productName;
    
    @Column(name = "quantity") @NonNull
    private Integer quantity;
    
    @Column(name="product_price")  
    private BigDecimal productPrice;
    
    @Column(name="discount_amount")  
    private BigDecimal discountAmount;
    
    @Column(name = "subtotal") @NonNull
    private BigDecimal subtotal;
}