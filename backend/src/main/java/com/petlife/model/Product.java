package com.petlife.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Product")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Integer productId;
    
    @Transient
    private List<Integer> categoryIds = new ArrayList<>();
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "Product_Category_Mapping",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<Category> categories = new ArrayList<>();
    
    @Column(name = "product_name" , nullable = false , length = 100)
    private String productName;
    
    @Column(name = "product_price" , nullable = false)
    private BigDecimal productPrice;
    
    @Column(name = "product_description" , columnDefinition = "TEXT", length = 2000)
    private String productDescription;
    
    @Column(name = "product_image" , length = 255)
    private String productImage;
    
    @Column(name = "product_stock")
    private Integer productStock;
    
    @Column(name = "low_stock")
    private Integer lowStock;
    
    @Column(name = "storage_position")
    private String storagePosition;
    
    @Column(name = "product_status")
    private Integer productStatus;
    
    // 保留：組員新增的點擊次數
    @Column(name = "click_count")
    private Integer clickCount = 0;
    
    @Transient
    private String categoryName;

    //  補回：我們之前實作的活動徽章
    @Transient
    private String activityBadge;
    
 // ✨ 新增：供前端判斷主商品或加購品的分區依據
    @jakarta.persistence.Transient
    private String productRole;

    // ✨ 新增：供前端智慧判定目前活動種類 (1, 2, 3, 4, 5)
    @jakarta.persistence.Transient
    private String discountType;
    // 保留：組員新增的多張圖片關聯
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    private List<ProductImage> images = new java.util.ArrayList<>();

    // Getter / Setter ... (省略其餘 Getter/Setter 以節省空間，請確保包含 activityBadge 的 Getter/Setter)
    public String getActivityBadge() { return activityBadge; }
    public void setActivityBadge(String activityBadge) { this.activityBadge = activityBadge; }
    
 //  discountType 的 Getter 與 Setter，ProductService 才能順暢調用
    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public String getProductRole() {
        return productRole;
    }

    public void setProductRole(String productRole) {
        this.productRole = productRole;
    }
}
