package com.petlife.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Heart")
@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
public class Heart {
	
	@jakarta.persistence.Transient // 不存進資料庫
	private Product product;
	
	@Id
	@Column(name="heart_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer heartId;
		
	@Column(name="member_id")
	@NotNull
	private Integer memberId;
	
	@Column(name="product_id")
	@NotNull
	private Integer productId;
		
	@Column(name="tracked_price")
	private BigDecimal trackedPrice;
	
	@Column(name="created_at", insertable = false, updatable = false)@NonNull
	private LocalDateTime createdAt;
	
	@Transient
	private BigDecimal currentPrice;
	
	@Transient
	private Boolean isPriceChange;

	public void setProduct(Product product) {
	    this.product = product;
	}

	public Product getProduct() {
	    return this.product;
	}
}
