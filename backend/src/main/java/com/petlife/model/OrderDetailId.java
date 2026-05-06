package com.petlife.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


//複合主鍵類別
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailId implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer orderBean; 
    private Integer productId;
}