package com.petlife.service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petlife.model.Order;
import com.petlife.model.OrderDetail;
import com.petlife.model.OrderDiscount;
import com.petlife.model.OrderPaymentRecord;
import com.petlife.model.Member; // ✨ 新增/修改：引入 Member 實體
import com.petlife.repository.CartItemRepository;
import com.petlife.repository.CartItemDTO;
import com.petlife.repository.CartCalculateResponseDTO;
import com.petlife.repository.DiscountDetailDTO;
import com.petlife.repository.OrderDetailRepository;
import com.petlife.repository.OrderPaymentRecordRepository;
import com.petlife.repository.OrderRepository;
import com.petlife.repository.ProductRepository;
import com.petlife.repository.DiscountRepository;
import com.petlife.repository.MemberRepository; // ✨ 新增/修改：引入 MemberRepository
import com.petlife.model.Discount;
import com.petlife.model.Product;
import com.petlife.service.DiscountEngine;
import com.petlife.service.OrderDiscountService;

@Service
public class OrderService {

	@Autowired
	private OrderRepository or;

	@Autowired
	private OrderDetailRepository odr;

	@Autowired
	private CartItemRepository cir;

	@Autowired
	private OrderPaymentRecordRepository prr;

	@Autowired
	private ProductRepository productRepository;

	// ✨ 新增/修改：注入 MemberRepository 以執行原子化扣點
	@Autowired
	private MemberRepository memberRepository;

	// --- 活動折扣相關：注入所需服務 ---
	@Autowired
	private DiscountEngine discountEngine;

	@Autowired
	private DiscountRepository discountRepository;

	@Autowired
	private OrderDiscountService orderDiscountService;
	// --- 活動折扣相關結束 ---

	// 綠界測試環境常數設定
	private final String MERCHANT_ID = "3002607";
	private final String HASH_KEY = "pwFHCqoQZGmho4w6";
	private final String HASH_IV = "EkRm7iFT261dpevs";
	private final String SERVICE_URL = "https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5";

	/**
	 * 執行完整結帳邏輯，包含扣點與綠界明細串接
	 */
	@Transactional
	public String processCheckout(Order order, Integer cartId) {
		// 計算購物車總金額（原始金額）
		BigDecimal totalAmount = or.getCartTotal(order.getMemberId());

		// 取得前端傳入的預計扣除點數
		Integer pointsToUse = order.getUsedPoint() != null ? order.getUsedPoint() : 0;

		// 預設最終付款金額 = 原始金額
		BigDecimal finalPaymentAmount = totalAmount;
		List<DiscountDetailDTO> appliedDiscountsForEcPay = new ArrayList<>();

		// 2026-05-14 修正：準備用來存折扣明細的暫存 List
		List<OrderDiscount> pendingOrderDiscounts = new ArrayList<>();

		// --- 2026-05-14 修正：將折扣計算移到 Order 存檔之前 ---
		try {
			List<com.petlife.model.CartItem> cartItemList = cir.findByCartId(cartId);
			if (cartItemList != null && !cartItemList.isEmpty()) {
				List<CartItemDTO> dtoList = new ArrayList<>();
				for (com.petlife.model.CartItem ci : cartItemList) {
					CartItemDTO dto = new CartItemDTO();
					dto.setItemId(ci.getItemId());
					dto.setProductId(ci.getProductId());
					dto.setPrice(ci.getProductPrice());
					dto.setQuantity(ci.getQuantity());
					Product p = productRepository.findById(ci.getProductId()).orElse(null);
					if (p != null && p.getCategories() != null && !p.getCategories().isEmpty()) {
						Integer specificCatId = p.getCategories().stream()
								.filter(c -> c.getCategoryType() != null && c.getCategoryType() == 1)
								.map(com.petlife.model.Category::getCategoryId).findFirst()
								.orElse(p.getCategories().iterator().next().getCategoryId());
						dto.setCategoryId(specificCatId);
					}
					dtoList.add(dto);
				}
				java.time.LocalDate today = java.time.LocalDate.now();
				List<Discount> activeDiscounts = discountRepository.findAll().stream()
						.filter(d -> "active".equals(d.getStatus()))
						.filter(d -> d.getStartDate() != null && d.getEndDate() != null)
						.filter(d -> !today.isBefore(d.getStartDate()) && !today.isAfter(d.getEndDate()))
						.collect(Collectors.toList());

				CartCalculateResponseDTO calcResult = discountEngine.executeDiscount(dtoList, activeDiscounts);
				BigDecimal discountTotal = calcResult.getDiscountAmount();

				if (discountTotal != null && discountTotal.compareTo(BigDecimal.ZERO) > 0) {
					finalPaymentAmount = totalAmount.subtract(discountTotal);
					if (finalPaymentAmount.compareTo(BigDecimal.ZERO) < 0)
						finalPaymentAmount = BigDecimal.ZERO;
					System.out.println("後端活動修改：折扣總金額" + discountTotal + "，最終付款金額" + finalPaymentAmount);
				}

				if (calcResult.getAppliedDiscounts() != null && !calcResult.getAppliedDiscounts().isEmpty()) {
					appliedDiscountsForEcPay.addAll(calcResult.getAppliedDiscounts());
					for (DiscountDetailDTO detail : calcResult.getAppliedDiscounts()) {
						for (Discount discount : activeDiscounts) {
							if (discount.getDiscountName().equals(detail.getName())) {
								OrderDiscount od = new OrderDiscount();
								if (!dtoList.isEmpty()) {
									od.setProductId(dtoList.get(0).getProductId());
								}
								od.setDiscountId(discount.getDiscountId());
								od.setQuantity(dtoList.stream().mapToInt(CartItemDTO::getQuantity).sum());
								od.setDiscountAmount(detail.getAmount());
								pendingOrderDiscounts.add(od);
								break;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("警告: 折扣計算失敗 " + e.getMessage());
		}

		// 進一步扣除紅利點數 (1點 = NT$1)
		if (pointsToUse > 0) {
			finalPaymentAmount = finalPaymentAmount.subtract(new BigDecimal(pointsToUse));
			if (finalPaymentAmount.compareTo(BigDecimal.ZERO) < 0)
				finalPaymentAmount = BigDecimal.ZERO;
		}

		// 除會員點數，若失敗則回滾
		if (pointsToUse > 0) {
			int rowsAffected = memberRepository.deductBonusPoints(order.getMemberId(), pointsToUse);
			if (rowsAffected == 0) {
				// 如果點數不足，丟出異常觸發 @Transactional Rollback
				throw new RuntimeException("紅利點數不足，請重新確認。");
			}
		}

		// 獲取扣點後的最新餘額並存入訂單紀錄
		Member currentMember = memberRepository.findById(order.getMemberId()).orElseThrow();
		order.setRemainingPoint(currentMember.getBonusPoints());
		order.setOrderTotal(finalPaymentAmount);

		// 建立訂單，取得 orderId
		Order savedOrder = or.save(order);
		Integer orderId = savedOrder.getOrderId();

		// 取得 orderId 後，補上關聯並存入明細表
		if (!pendingOrderDiscounts.isEmpty()) {
			for (OrderDiscount od : pendingOrderDiscounts) {
				od.setOrderId(orderId);
			}
			orderDiscountService.saveAllOrderDiscounts(pendingOrderDiscounts);
		}

		// 轉移購物車資料到訂單明細
		odr.transferCartToOrderDetails(orderId, order.getMemberId());

		// 將折扣金額寫回 OrderDetail ---
		try {
			List<OrderDetail> newlyCreatedDetails = odr.findByOrderBean_OrderId(orderId);
			if (newlyCreatedDetails != null && !newlyCreatedDetails.isEmpty() && !pendingOrderDiscounts.isEmpty()) {
				for (OrderDetail detail : newlyCreatedDetails) {
					for (OrderDiscount od : pendingOrderDiscounts) {
						if (detail.getProductId().equals(od.getProductId())) {
							detail.setDiscountAmount(od.getDiscountAmount());
							BigDecimal newSubtotal = detail.getSubtotal().subtract(od.getDiscountAmount());
							detail.setSubtotal(
									newSubtotal.compareTo(BigDecimal.ZERO) > 0 ? newSubtotal : BigDecimal.ZERO);
							break;
						}
					}
				}
				odr.saveAll(newlyCreatedDetails);
			}
		} catch (Exception e) {
			System.out.println("警告: 更新 OrderDetail 折扣金額失敗 " + e.getMessage());
		}

		// 清空購物車資料
		cir.deleteByCartId(cartId);

		// 建立並儲存金流紀錄
		String merchantTradeNo = "PET" + orderId + "T" + (System.currentTimeMillis() % 10000);
		OrderPaymentRecord record = new OrderPaymentRecord();
		record.setOrder(savedOrder);
		record.setMerchantTradeNo(merchantTradeNo);
		record.setPaymentAmt(finalPaymentAmount);
		record.setPaymentType(order.getOrderPayment());
		prr.save(record);

		// ✨ 新增/修改：將點數折抵資訊傳入表單產出方法
		return generateEcPayForm(savedOrder, merchantTradeNo, finalPaymentAmount, appliedDiscountsForEcPay,
				pointsToUse);
	}

	/**
	 * 產出包含點數折抵明細的綠界跳轉表單
	 */
	private String generateEcPayForm(Order order, String merchantTradeNo, BigDecimal finalAmount,
			List<DiscountDetailDTO> appliedDiscounts, Integer pointsUsed) {
		Map<String, String> params = new TreeMap<>();
		params.put("MerchantID", MERCHANT_ID);
		params.put("MerchantTradeNo", merchantTradeNo);
		params.put("MerchantTradeDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
		params.put("PaymentType", "aio");
		params.put("TotalAmount", String.valueOf(finalAmount.intValue()));
		params.put("TradeDesc", "PetLifeOrder");

		StringBuilder itemNameBuilder = new StringBuilder("PetLifeOrder");

		// 串接活動折扣明細
		if (appliedDiscounts != null && !appliedDiscounts.isEmpty()) {
			for (DiscountDetailDTO d : appliedDiscounts) {
				itemNameBuilder.append("#").append(d.getName()).append(" -NT$").append(d.getAmount().intValue());
			}
		}

		// 在綠界項目清單中加入紅利折抵項目
		if (pointsUsed != null && pointsUsed > 0) {
			itemNameBuilder.append("#紅利點數折抵 -NT$").append(pointsUsed);
		}

		params.put("ItemName", itemNameBuilder.toString());
		params.put("ChoosePayment", "ALL");
		params.put("EncryptType", "1");
		params.put("ClientBackURL", "http://localhost:5174/checkoutsuccess");
		params.put("ReturnURL", "https://enable-impeach-caress.ngrok-free.dev/api/payment/callback");
		params.put("NeedExtraPaidInfo", "N");

		// 計算加密簽章CheckMacValue
		String checkMacValue = calculateCheckMacValue(params);
		params.put("CheckMacValue", checkMacValue);

		StringBuilder form = new StringBuilder();
		form.append("<form id=\"ecpayForm\" action=\"").append(SERVICE_URL).append("\" method=\"post\">");
		for (Map.Entry<String, String> entry : params.entrySet()) {
			form.append("<input type=\"hidden\" name=\"").append(entry.getKey()).append("\" value=\"")
					.append(entry.getValue()).append("\">");
		}
		form.append("</form>");
		form.append("<script>document.getElementById(\"ecpayForm\").submit();</script>");

		return form.toString();
	}

	/**
	 * 回傳結果包含最新會員資訊，供前端 Store 即時同步
	 */
	@Transactional
	public Map<String, Object> processCheckoutAndReturnDetail(Order order, Integer cartId) {
		String ecpayForm = processCheckout(order, cartId);

		// 獲取扣點後的最新會員物件
		Member updatedMember = memberRepository.findById(order.getMemberId()).orElse(null);

		Map<String, Object> response = new java.util.HashMap<>();
		response.put("order", order);
		response.put("form", ecpayForm);
		response.put("member", updatedMember); // 回傳會員物件

		return response;
	}

	private String calculateCheckMacValue(Map<String, String> params) {
		// 串接原始字串
		String rawData = "HashKey=" + HASH_KEY + "&"
				+ params.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("&"))
				+ "&HashIV=" + HASH_IV;

		// 綠界規定的URL Encode
		String encodedData = URLEncoder.encode(rawData, StandardCharsets.UTF_8).toLowerCase().replace("%21", "!")
				.replace("%2a", "*").replace("%28", "(").replace("%29", ")");

		// SHA256加密
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(encodedData.getBytes(StandardCharsets.UTF_8));
			StringBuilder hexString = new StringBuilder();
			for (byte b : hash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString().toUpperCase();
		} catch (Exception ex) {
			throw new RuntimeException("加密失敗: " + ex.getMessage());
		}
	}

	// 接收綠界回傳的通知，自動同步訂單狀態

	// 處理綠界回傳的通知，自動化同步訂單狀態
	@Transactional
	public void updateOrderPaymentStatus(String merchantTradeNo) {
		OrderPaymentRecord record = prr.findByMerchantTradeNo(merchantTradeNo).orElse(null);

		if (record != null) {
			// 查到對應的紀錄，代表付款成功，更新訂單狀態
			Order order = record.getOrder();

			// 狀態改為1
			order.setOrderStatus("1");
			or.save(order);

			System.out.println("訂單編號 " + order.getOrderId() + " 狀態更新成功");
		} else {
			System.out.println("找不到對應的交易紀錄: " + merchantTradeNo);
		}
	}

	// 結帳成功要抓資料用的
	public Map<String, Object> getOrderDetailWithItems(Integer orderId) {
		Order order = or.findById(orderId).orElse(null);
		if (order == null)
			return null;

		Map<String, Object> result = new HashMap<>();
		result.put("orderId", order.getOrderId());
		result.put("orderDate", order.getOrderDate());
		result.put("orderAddress", order.getOrderAddress());
		result.put("orderName", order.getOrderName());
		result.put("orderTotal", order.getOrderTotal());

		// 查詢該訂單的所有明細
		List<OrderDetail> details = odr.findByOrderBean_OrderId(orderId);

		List<Map<String, Object>> itemsList = new ArrayList<>();
		if (details != null && !details.isEmpty()) {
			for (OrderDetail detail : details) {
				Map<String, Object> itemMap = new HashMap<>();

				// 這裡請對應資料庫 OrderDetail 表的欄位名稱
				itemMap.put("productId", detail.getProductId());
				itemMap.put("productName", detail.getProductName());
				itemMap.put("productPrice", detail.getProductPrice());
				itemMap.put("quantity", detail.getQuantity());
				itemMap.put("subtotal", detail.getSubtotal());
				itemMap.put("discount",
						detail.getDiscountAmount() != null ? detail.getDiscountAmount() : BigDecimal.ZERO);

				// Fetch categoryId for the frontend to call calculate API
				Integer catId = 0;
				if (detail.getProductId() != null) {
					Product p = productRepository.findById(detail.getProductId()).orElse(null);
					if (p != null && p.getCategories() != null && !p.getCategories().isEmpty()) {
						catId = p.getCategories().get(0).getCategoryId();
					}
				}
				itemMap.put("categoryId", catId);

				itemsList.add(itemMap);
			}
		} else {
			System.out.println("警告：資料庫中找不到訂單編號 " + orderId + " 的任何明細");
		}

		result.put("items", itemsList);
		return result;
	}

	// 查詢該會員的所有訂單(未刪除的)
	public List<Order> findByMemberId(Integer memberId) {
		return or.findByMemberIdAndIsDeletedFalseOrderByOrderDateDesc(memberId);
	}

	// 查詢單筆訂單
	public Order findById(Integer orderId) {
		// findById 回傳的是 Optional，所以用 .orElse(null) 表示找不到就回傳 null
		return or.findById(orderId).orElse(null);
	}

	// 新增/更新訂單（取消訂單狀態要存回去）
	public void save(Order order) {
		or.save(order);
	}

	// 會員的紅利明細 (不需額外建表)

	public List<Map<String, Object>> getBonusHistory(Integer memberId) {
		// 取得該會員所有未刪除的訂單
		List<Order> orders = findByMemberId(memberId);

		// 將訂單依時間「由舊到新」排序，以便正向累加餘額
		orders.sort((a, b) -> a.getOrderDate().compareTo(b.getOrderDate()));

		List<Map<String, Object>> history = new ArrayList<>();
		int currentBalance = 0; // 存摺餘額模擬

		for (Order order : orders) {
			// 1. 【消耗紀錄】結帳使用的點數
			if (order.getUsedPoint() != null && order.getUsedPoint() > 0) {
				currentBalance -= order.getUsedPoint();
				Map<String, Object> useRecord = new HashMap<>();
				useRecord.put("orderId", order.getOrderId());
				useRecord.put("type", "消耗");
				useRecord.put("description", "購物折抵");
				useRecord.put("points", -order.getUsedPoint());
				useRecord.put("balance", currentBalance);
				useRecord.put("date", order.getOrderDate());
				useRecord.put("orderStatus", order.getOrderStatus()); // ✨ 新增/修改：向前端傳遞訂單狀態以供連動顯示
				history.add(useRecord);

				// 2. 【退回紀錄】訂單取消後的紅利退回
				if ("已取消".equals(order.getOrderStatus())) {
					currentBalance += order.getUsedPoint();
					Map<String, Object> refundRecord = new HashMap<>();
					refundRecord.put("orderId", order.getOrderId());
					refundRecord.put("type", "獲取");
					refundRecord.put("description", "取消退回");
					refundRecord.put("points", order.getUsedPoint());
					refundRecord.put("balance", currentBalance);
					refundRecord.put("date", order.getOrderDate().plusSeconds(1)); // 避免同秒排序問題
					refundRecord.put("orderStatus", order.getOrderStatus()); // ✨ 新增/修改：向前端傳遞訂單狀態以供連動顯示
					history.add(refundRecord);
				}
			}

			// 3. 【獲取紀錄】訂單完成發放的紅利 (1%)
			// ✨ 新增/修改：改為所有未刪除訂單皆會產生回饋明細，但僅在狀態為「已完成」時才累加至實質 currentBalance 餘額中
			if (order.getOrderTotal() != null) {
				int earned = order.getOrderTotal().multiply(new BigDecimal("0.01")).intValue();
				if (earned > 0) {
					// 只有當訂單真正「已完成」，該筆點數才會實質計入使用者的存摺可用餘額
					if ("已完成".equals(order.getOrderStatus())) {
						currentBalance += earned;
					}
					Map<String, Object> earnRecord = new HashMap<>();
					earnRecord.put("orderId", order.getOrderId());
					earnRecord.put("type", "獲取");
					earnRecord.put("description", "訂單回饋");
					earnRecord.put("points", earned);
					earnRecord.put("balance", currentBalance);
					earnRecord.put("date", order.getOrderDate());
					earnRecord.put("orderStatus", order.getOrderStatus()); // ✨ 新增/修改：向前端傳遞訂單狀態以供連動顯示
					history.add(earnRecord);
				}
			}
		}

		// 依日期「由新到舊」重新排序，供前端存摺顯示
		history.sort((a, b) -> ((LocalDateTime) b.get("date")).compareTo((LocalDateTime) a.get("date")));
		return history;
	}

	// 取消訂單並執行退回點數
	@Transactional
	public void cancelOrder(Integer orderId) {
		Order order = or.findById(orderId).orElseThrow(() -> new RuntimeException("找不到訂單"));
		if ("已取消".equals(order.getOrderStatus()) || "已完成".equals(order.getOrderStatus())) {
			throw new RuntimeException("目前狀態不允許取消");
		}
		order.setOrderStatus("已取消");
		if (order.getUsedPoint() != null && order.getUsedPoint() > 0) {
			memberRepository.addBonusPoints(order.getMemberId(), order.getUsedPoint());
		}
		or.save(order);
	}

	// 確認收貨並發放紅利點數
	@Transactional
	public void completeOrder(Integer orderId) {
		Order order = or.findById(orderId).orElseThrow(() -> new RuntimeException("找不到訂單"));
		if (!"已完成".equals(order.getOrderStatus()) && !"已取消".equals(order.getOrderStatus())) {
			order.setOrderStatus("已完成");
			int earnedPoints = order.getOrderTotal().multiply(new BigDecimal("0.01")).intValue();
			if (earnedPoints > 0) {
				memberRepository.addBonusPoints(order.getMemberId(), earnedPoints);
			}
			or.save(order);
		}
	}
}
