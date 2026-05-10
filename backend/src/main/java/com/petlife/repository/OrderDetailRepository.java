package com.petlife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.petlife.model.OrderDetail;
import com.petlife.model.OrderDetailId;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, OrderDetailId> {

    // 透過orderId查詢明細
    List<OrderDetail> findByOrderBean_OrderId(Integer orderId);
    
    // 刪除某訂單的所有明細
    void deleteByOrderBean_OrderId(Integer orderId);

    // 從購物車直接轉移至訂單明細
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO OrderDetail (order_id, product_id, product_name, quantity, product_price, discount_amount, subtotal) "
            + "SELECT :orderId, ci.product_id, ci.product_name, ci.quantity, ci.product_price, 0, (ci.product_price * ci.quantity) "
            + "FROM cartItem ci "
            + "JOIN Cart c ON ci.cart_id = c.cart_id "
            + "WHERE c.member_id = :memberId", nativeQuery = true)
    void transferCartToOrderDetails(@Param("orderId") int orderId, @Param("memberId") Integer memberId);
    
}