package com.petlife.service;

import com.petlife.model.Order;
import com.petlife.model.OrderDetail; // 假設明細 Bean 名稱
import com.petlife.repository.OrderRepository;
import com.petlife.repository.OrderDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class OrderAdminService {

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderDetailRepository orderDetailRepository;

	// 取得所有訂單
	public List<Order> findActiveOrders(String search) {
		if (search != null && !search.trim().isEmpty()) {
			return orderRepository.findByOrderNameContainingAndIsDeletedFalseOrderByOrderDateAsc(search);
		}
		return orderRepository.findByIsDeletedFalseOrderByOrderDateDesc();
	}

	// 取得訂單詳細明細
	public Map<String, Object> getOrderWithDetails(Integer id) {
		Map<String, Object> map = new HashMap<>();
		Order order = orderRepository.findById(id).orElse(null);
		List<OrderDetail> details = orderDetailRepository.findByOrderBean_OrderId(id);

		List<Map<String, Object>> detailList = new ArrayList<>();
		for (OrderDetail d : details) {
			Map<String, Object> item = new HashMap<>();
			item.put("productName", d.getProductName());
			item.put("productPrice", d.getProductPrice());
			item.put("quantity", d.getQuantity());
			item.put("subtotal", d.getSubtotal());
			detailList.add(item);
		}

		map.put("order", order);
		map.put("details", detailList);
		return map;
	}

	// 更新訂單
	public void updateStatusAndPayment(Integer id, String status, String payment) {
		orderRepository.findById(id).ifPresent(o -> {
			o.setOrderStatus(status);
			o.setOrderPayment(payment);
			orderRepository.save(o);
		});
	}

	// 軟刪除
	public boolean performSoftDelete(Integer id) {
		return orderRepository.findById(id).map(o -> {
			o.setIsDeleted(true);
			orderRepository.save(o);
			return true;
		}).orElse(false);
	}

	// 🎯 1. 取得結帳方式佔比統計
	public Map<String, Long> getPaymentAnalysis() {
		List<Map<String, Object>> rawData = orderRepository.countOrdersByPaymentMethod();
		Map<String, Long> resultMap = new HashMap<>();

		// 初始化預設值為 0
		resultMap.put("creditCard", 0L);
		resultMap.put("linePay", 0L);
		resultMap.put("transfer", 0L);
		resultMap.put("cod", 0L);

		for (Map<String, Object> row : rawData) {
			String payment = (String) row.get("payment");
			Long count = ((Number) row.get("count")).longValue();

			if (payment == null)
				continue;
			// 根據你資料庫真正儲存的付款字串去做 Mapping (以下為假設，可自行替換)
			if (payment.equalsIgnoreCase("CREDIT_CARD") || payment.contains("信用卡")) {
				resultMap.put("creditCard", count);
			} else if (payment.equalsIgnoreCase("TRANSFER") || payment.contains("金融卡")) {
				resultMap.put("transfer", count);
			} else if (payment.equalsIgnoreCase("LINE_PAY") || payment.contains("LinePay")) {
				resultMap.put("linePay", count);
			}
		}
		return resultMap;
	}

	// 取得近一個月內訂單狀態統計
	public Map<String, Long> getRecentStatusAnalysis() {
		LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(30);
		List<Map<String, Object>> rawData = orderRepository.countRecentOrdersByStatus(oneMonthAgo);
		Map<String, Long> resultMap = new HashMap<>();

		// 初始化預設值
		resultMap.put("completed", 0L);
		resultMap.put("pending", 0L);
		resultMap.put("cancelled", 0L);

		for (Map<String, Object> row : rawData) {
			String status = (String) row.get("status");
			Long count = ((Number) row.get("count")).longValue();

			if (status == null)
				continue;
			// 根據資料庫真正儲存的狀態字串去做 Mapping
			if (status.equalsIgnoreCase("COMPLETED") || status.contains("已完成")) {
				resultMap.put("completed", count);
			} else if (status.equalsIgnoreCase("PENDING") || status.contains("處理中")) {
				resultMap.put("pending", count);
			} else if (status.equalsIgnoreCase("CANCELLED") || status.contains("已取消")) {
				resultMap.put("cancelled", count);
			}
		}
		return resultMap;
	}

	// 取得每月訂單趨勢走勢
	public List<Map<String, Object>> getOrderTrendAnalysis() {
		List<Map<String, Object>> rawData = orderRepository.countOrdersByMonthTrendGrouped();
		List<Map<String, Object>> resultList = new ArrayList<>();

		for (Map<String, Object> row : rawData) {
			Map<String, Object> item = new HashMap<>();
			item.put("month", row.get("month"));

			// 轉出一般訂單數與活動訂單數
			item.put("normalCount",
					row.get("normalCount") != null ? ((Number) row.get("normalCount")).longValue() : 0L);
			item.put("promoCount", row.get("promoCount") != null ? ((Number) row.get("promoCount")).longValue() : 0L);

			resultList.add(item);
		}
		return resultList;
	}

	// 根據條件過濾訂單（前端點擊圖表彈出Modal時使用）
	public List<Order> findOrdersByCondition(String searchType, String keyword) {
		if ("paymentMethod".equals(searchType)) {
			return orderRepository.findByOrderPaymentAndIsDeletedFalseOrderByOrderDateDesc(keyword);
		} else if ("orderStatus".equals(searchType)) {
			return orderRepository.findByOrderStatusAndIsDeletedFalseOrderByOrderDateDesc(keyword);
		}
		return orderRepository.findByIsDeletedFalseOrderByOrderDateDesc();
	}
}
