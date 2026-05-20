package com.petlife.model;

import java.io.Serializable;

import org.hibernate.annotations.Formula;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 							// 宣告這是一個資料庫實體
@Table(name = "Category") 		// 對應 SQL Server 的資料表名稱
@Data 							// 自動產生 Getter, Setter, toString, equals, hashCode
@NoArgsConstructor 				// 自動產生無參數建構子
@AllArgsConstructor 				// 自動產生全參數建構子

public class Category implements Serializable {
    private static final long serialVersionUID = 1L;

//商品分類編號 (主鍵)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

//商品分類名稱
    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

//分類類型 (1: 實體分類, 2: 大專區, 3: 活動標籤)
    @Column(name = "category_type", nullable = false)
    private Integer categoryType = 1;

//父分類編號 (可用於層級關係)
    @Column(name = "parent_id")
    private Integer parentId;

//商品數量 (非資料庫欄位，用於查詢統計)
 // Hibernate 的動態計算 @Formula
    @Formula("(SELECT COUNT(*) FROM Product_Category_Mapping pcm WHERE pcm.category_id = category_id)")
    private Integer productCount;

//自訂排序順序
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
    
 
    // ✨ 用於傳遞給前端，標示此標籤是否在消費者前台被自動隱藏 (不存入實體資料庫)
    @Transient
    private Boolean isHiddenInFront = false;
}


