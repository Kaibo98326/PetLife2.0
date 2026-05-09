package com.petlife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.petlife.model.Order;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // 查詢歷史訂單，按會員id降序
    List<Order> findByMemberIdOrderByOrderIdDesc(Integer memberId);

    // 查詢歷史訂單：按日期降序
    List<Order> findAllByOrderByOrderDateDesc();

    // 模糊查詢收件人姓名
    List<Order> findByOrderNameContainingOrderByOrderDateAsc(String orderName);

    // 計算該會員目前的購物車總額
    @Query("SELECT SUM(ci.productPrice * ci.quantity) " +
           "FROM CartItem ci " +
           "JOIN Cart c ON ci.cartId = c.cartId " +
           "WHERE c.memberId = :memberId")
    BigDecimal getCartTotal(@Param("memberId") Integer memberId);

    // 取得特定訂單總額(綠界驗證用)
    @Query("SELECT o.orderTotal FROM Order o WHERE o.orderId = :orderId")
    BigDecimal findOrderTotalById(@Param("orderId") Integer orderId);
    
    List<Order> findByMemberIdAndIsDeletedFalseOrderByOrderDateDesc(Integer memberId);

	List<Order> findByOrderNameContainingAndIsDeletedFalseOrderByOrderDateAsc(String search);

	List<Order> findByIsDeletedFalseOrderByOrderDateDesc();

}
