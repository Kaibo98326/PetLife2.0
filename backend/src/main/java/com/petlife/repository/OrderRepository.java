package com.petlife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.petlife.model.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

	// 查詢歷史訂單，按會員id降序
	List<Order> findByMemberIdOrderByOrderIdDesc(Integer memberId);

	// 查詢歷史訂單：按日期降序
	List<Order> findAllByOrderByOrderDateDesc();

	// 模糊查詢收件人姓名
	List<Order> findByOrderNameContainingOrderByOrderDateAsc(String orderName);

	// 計算該會員目前的購物車總額
	@Query("SELECT SUM(ci.productPrice * ci.quantity) " + "FROM CartItem ci " + "JOIN Cart c ON ci.cartId = c.cartId "
			+ "WHERE c.memberId = :memberId")
	BigDecimal getCartTotal(@Param("memberId") Integer memberId);

	// 取得特定訂單總額(綠界驗證用)
	@Query("SELECT o.orderTotal FROM Order o WHERE o.orderId = :orderId")
	BigDecimal findOrderTotalById(@Param("orderId") Integer orderId);

	List<Order> findByMemberIdAndIsDeletedFalseOrderByOrderDateDesc(Integer memberId);

	List<Order> findByOrderNameContainingAndIsDeletedFalseOrderByOrderDateAsc(String search);

	List<Order> findByIsDeletedFalseOrderByOrderDateDesc();

	// 統計結帳方式筆數
	@Query(value = "SELECT order_payment as payment, COUNT(*) as count " + "FROM [Order] " + "WHERE is_deleted = 0 "
			+ "GROUP BY order_payment", nativeQuery = true)
	List<Map<String, Object>> countOrdersByPaymentMethod();

	// 統計近一個月內特定狀態的訂單筆數
	@Query(value = "SELECT order_status as status, COUNT(*) as count " + "FROM [Order] "
			+ "WHERE is_deleted = 0 AND order_date >= :oneMonthAgo " + "GROUP BY order_status", nativeQuery = true)
	List<Map<String, Object>> countRecentOrdersByStatus(@Param("oneMonthAgo") LocalDateTime oneMonthAgo);

	// 按月份統計訂單趨勢
	@Query(value = "SELECT FORMAT(order_date, 'yyyy-MM') as month, COUNT(*) as count " + "FROM [Order] "
			+ "WHERE is_deleted = 0 " + "GROUP BY FORMAT(order_date, 'yyyy-MM') "
			+ "ORDER BY month ASC", nativeQuery = true)
	List<Map<String, Object>> countOrdersByMonthTrend();

	// 給前端Modal點擊圖表後，進行條件篩選訂單的擴充查詢
	List<Order> findByOrderPaymentAndIsDeletedFalseOrderByOrderDateDesc(String payment);

	List<Order> findByOrderStatusAndIsDeletedFalseOrderByOrderDateDesc(String status);

}
