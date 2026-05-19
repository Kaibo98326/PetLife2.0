package com.petlife.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.petlife.model.Order;
import com.petlife.repository.CheckoutRequsetDTO;
import com.petlife.service.JwtUtils;
import com.petlife.service.OrderService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	@Autowired
	private OrderService orderService;

	@Autowired
	private JwtUtils jwtUtils;

	@PostMapping("/checkout")
	public ResponseEntity<Map<String, Object>> checkout(@RequestBody CheckoutRequsetDTO requset,
													   @RequestParam Integer cartId) {
		// 呼叫 Service 取得包含更新後會員資料的結果(存訂單、搬明細、清購物車、算加密)
		Map<String, Object> result = 
				orderService.processCheckoutAndReturnDetail(requset.getOrder(), cartId,
														   requset.getAppliedDiscounts(),requset.getCartItems());

		// 2. 回傳給 Vue
		return ResponseEntity.ok(result);
	}

	@GetMapping("/detail/{orderId}")
	public ResponseEntity<Map<String, Object>> getOrderDetail(@PathVariable Integer orderId) {
		Map<String, Object> orderDetail = orderService.getOrderDetailWithItems(orderId);

		if (orderDetail != null) {
			return ResponseEntity.ok(orderDetail);
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	// 會員查詢歷史訂單
	@GetMapping("/historyorders")
	public ResponseEntity<?> getHistoryOrders(@RequestHeader("Authorization") String token) {
		try {
			// 從Header提取並解析JWT
			String jwt = token.replace("Bearer ", "");
			String memberIdStr = jwtUtils.validateToken(jwt); // 解析出 MemberId (String)

			if (memberIdStr == null || memberIdStr.isEmpty()) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("無效的憑證");
			}

			Integer mid = Integer.valueOf(memberIdStr);
			System.out.println("✅ JWT 驗證成功，會員 ID: " + mid);

			// 執行查詢
			List<Order> orders = orderService.findByMemberId(mid);
			System.out.println("查詢完成，訂單筆數: " + orders.size());

			return ResponseEntity.ok(orders);

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("登入資訊已過期或錯誤");
		}
	}

	// 會員取消訂單用
	@PostMapping("/cancel/{orderId}")
	public ResponseEntity<?> cancelOrder(@PathVariable Integer orderId) {
		// 抓取訂單
		Order order = orderService.findById(orderId);

		if (order == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("系統找不到該筆訂單");
		}

		// 是否超過3天
		LocalDateTime now = LocalDateTime.now();
		if (order.getOrderDate().plusDays(3).isBefore(now)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("訂單已成立超過 3 天，無法取消");
		}

		// 檢查目前狀態是否允許取消(避免重複取消或取消已完成訂單)
		if ("已取消".equals(order.getOrderStatus())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("訂單已經是取消狀態");
		}

		if ("已完成".equals(order.getOrderStatus())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("訂單已完成，無法取消");
		}

		// 變更狀態並存檔
		try {
//			order.setOrderStatus("已取消");
//			orderService.save(order);

			// 呼叫 Service 執行包含退點
			orderService.cancelOrder(orderId);

			return ResponseEntity.ok("訂單取消成功");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("更新失敗：" + e.getMessage());
		}
	}

	// 會員確認收貨 API
	@PostMapping("/complete/{orderId}")
	public ResponseEntity<?> completeOrder(@PathVariable Integer orderId) {
		try {
			// 呼叫 Service 執行狀態變更與發放點數
			orderService.completeOrder(orderId);
			return ResponseEntity.ok("訂單已完成，紅利點數已發放");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
	}

	// 取得會員紅利點數明細 API
	@GetMapping("/bonus-history")
	public ResponseEntity<?> getBonusHistory(@RequestHeader("Authorization") String token) {
		try {
			// 從 Header 解析 JWT
			String jwt = token.replace("Bearer ", "");
			String memberIdStr = jwtUtils.validateToken(jwt);
			if (memberIdStr == null || memberIdStr.isEmpty()) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("無效的憑證");
			}

			Integer mid = Integer.valueOf(memberIdStr);
			// 呼叫 Service 取得動態聚合的明細
			List<Map<String, Object>> history = orderService.getBonusHistory(mid);
			return ResponseEntity.ok(history);

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("登入資訊已過期或錯誤");
		}
	}

	@GetMapping("/estimate")
	public ResponseEntity<?> estimateBonus(@RequestParam BigDecimal amount,
			@RequestHeader("Authorization") String token) {
		try {
			String jwt = token.replace("Bearer ", "");
			String memberIdStr = jwtUtils.validateToken(jwt);
			if (memberIdStr == null || memberIdStr.isEmpty()) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("無效的憑證");
			}

			// 呼叫 Service 統一的紅利生成邏輯
			int estimatedPoints = orderService.calculateEarnedBonus(amount);
			return ResponseEntity.ok(estimatedPoints);

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("計算紅利失敗");
		}
	}
}