package com.petlife.repository;

import com.petlife.model.OrderDiscount;
import com.petlife.model.OrderDiscountId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDiscountRepository extends JpaRepository<OrderDiscount, OrderDiscountId> {
    
    // 透過訂單編號查詢該訂單所有的折扣紀錄 (圖1管理介面用得到)
    // 活動修改：使用明確 JPQL 避免 @IdClass 複合主鍵下衍生查詢失效的問題
    @Query("SELECT od FROM OrderDiscount od WHERE od.orderId = :orderId")
    List<OrderDiscount> findByOrderId(@Param("orderId") Integer orderId);
    
    // 透過活動編號查詢有哪些訂單使用了這個折扣 (數據統計用)
    // 活動修改：使用明確 JPQL 避免 @IdClass 複合主鍵下 findByDiscountId 查不到資料的問題
    @Query("SELECT od FROM OrderDiscount od WHERE od.discountId = :discountId")
    List<OrderDiscount> findByDiscountId(@Param("discountId") Integer discountId);

    // 活動修改：JOIN Discount 表，回傳含活動名稱的摘要 DTO，供結帳成功頁顯示折抵明細
    @Query("SELECT new com.petlife.repository.OrderDiscountSummaryDTO(d.discountName, od.discountAmount) " +
           "FROM OrderDiscount od, Discount d " +
           "WHERE od.orderId = :orderId AND od.discountId = d.discountId")
    List<OrderDiscountSummaryDTO> findSummaryByOrderId(@Param("orderId") Integer orderId);
    
 // 用於接收高質感儀表板 JOIN 查詢結果的 Projection 介面
    public interface DiscountUsageProjection {
        Integer getOrderId();
        java.time.LocalDateTime getOrderDate();
        Integer getProductId();
        String getProductName();
        String getProductImage();
        Integer getQuantity();
        java.math.BigDecimal getDiscountAmount(); 
    }

    // JOIN 訂單與商品表，撈取高質感明細所需的完整圖文與日期資料
    @Query("SELECT od.orderId AS orderId, o.orderDate AS orderDate, od.productId AS productId, " +
           "p.productName AS productName, p.productImage AS productImage, " +
           "od.quantity AS quantity, od.discountAmount AS discountAmount " +
           "FROM OrderDiscount od, Order o, Product p " +
           "WHERE od.orderId = o.orderId AND od.productId = p.productId " +
           "AND od.discountId = :discountId " +
           "ORDER BY o.orderDate DESC")
    List<DiscountUsageProjection> findDiscountUsageDetails(@Param("discountId") Integer discountId);
}