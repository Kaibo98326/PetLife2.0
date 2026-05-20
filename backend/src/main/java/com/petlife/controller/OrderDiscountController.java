package com.petlife.controller;

import com.petlife.model.OrderDiscount;
import com.petlife.repository.OrderDiscountRepository;
import com.petlife.repository.OrderDiscountSummaryDTO;
import com.petlife.service.OrderDiscountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-discounts")
public class OrderDiscountController {

	@Autowired
	private OrderDiscountService orderDiscountService;

	/**
	 * 查詢某筆訂單的所有折扣明細 (這可以做在圖1後台，點擊查看活動使用情況時呼叫)
	 */
	@GetMapping("/order/{orderId}")
	public ResponseEntity<List<OrderDiscount>> getDiscountsByOrderId(@PathVariable Integer orderId) {
		List<OrderDiscount> discounts = orderDiscountService.getDiscountsByOrderId(orderId);
		return ResponseEntity.ok(discounts);
	}

	/**
	 * 手動/測試儲存訂單折扣明細
	 */
	@PostMapping("/save")
	public ResponseEntity<String> saveOrderDiscounts(@RequestBody List<OrderDiscount> discounts) {
		orderDiscountService.saveAllOrderDiscounts(discounts);
		return ResponseEntity.ok("訂單折扣紀錄儲存成功");
	}

	/**
	 * 查詢某個活動被哪些訂單使用 (後台活動明細用)
	 */
	@GetMapping("/discount/{discountId}")
	public ResponseEntity<List<OrderDiscount>> getDiscountsByDiscountId(@PathVariable Integer discountId) {
		List<OrderDiscount> discounts = orderDiscountService.getDiscountsByDiscountId(discountId);
		return ResponseEntity.ok(discounts);
	}

	/**
	 * 活動修改：查詢某筆訂單的折扣摘要（含活動名稱），供結帳成功頁顯示 不重新計算折扣，直接從 OrderDiscount 表讀取已儲存的折扣金額
	 */
	@GetMapping("/order/{orderId}/summary")
	public ResponseEntity<List<OrderDiscountSummaryDTO>> getDiscountSummaryByOrderId(@PathVariable Integer orderId) {
		List<OrderDiscountSummaryDTO> summary = orderDiscountService.getDiscountSummaryByOrderId(orderId);
		return ResponseEntity.ok(summary);
	}

	// 提供給高質感活動明細彈窗的專屬 API (帶有商品圖文與下單日期)
	@GetMapping("/discount/{discountId}/details")
	public ResponseEntity<List<OrderDiscountRepository.DiscountUsageProjection>> getDiscountUsageDetails(
			@PathVariable Integer discountId) {
		List<OrderDiscountRepository.DiscountUsageProjection> details = orderDiscountService
				.getDiscountUsageDetails(discountId);
		return ResponseEntity.ok(details);
	}
}