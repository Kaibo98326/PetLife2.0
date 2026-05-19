package com.petlife.service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import com.petlife.model.OrderPaymentRecord;
import com.petlife.model.Product;
import com.petlife.repository.CartItemRepository;
import com.petlife.repository.DiscountDetailDTO;
import com.petlife.repository.MemberRepository;
import com.petlife.repository.OrderDetailRepository;
import com.petlife.repository.OrderPaymentRecordRepository;
import com.petlife.repository.OrderRepository;
import com.petlife.repository.ProductRepository;

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
	private MemberRepository memberRepository;

	@Autowired
	private ProductRepository productRepository;

	// 綠界測試環境常數設定
	private final String MERCHANT_ID = "3002607";
	private final String HASH_KEY = "pwFHCqoQZGmho4w6";
	private final String HASH_IV = "EkRm7iFT261dpevs";
	private final String SERVICE_URL = "https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5";

	@Transactional
	public String processCheckout(Order order, Integer cartId, List<DiscountDetailDTO> appliedDiscounts) {
		// 計算購物車總金額 (這是原始小計)
		BigDecimal subTotal = or.getCartTotal(order.getMemberId());

		// 預先計算扣除折扣與紅利後的最終金額
		Integer pointsUsed = order.getUsedPoint() != null ? order.getUsedPoint() : 0;
		BigDecimal finalAmount = calculateFinalAmount(subTotal, appliedDiscounts, pointsUsed);

		// 將最終結帳金額存入訂單
		order.setOrderTotal(finalAmount);

		// 儲存訂單，取得orderId
		Order savedOrder = or.save(order);
		Integer orderId = savedOrder.getOrderId();

		// 先抓出該會員購物車裡的所有項目
		var cartItems = cir.findByCartId(cartId); // 或者是你對應抓購物車項目的方法

		if (cartItems.isEmpty()) {
			throw new RuntimeException("購物車是空的，無法結帳！");
		}

		List<OrderDetail> details = new ArrayList<>();
		for (var item : cartItems) {
			OrderDetail od = new OrderDetail();
			od.setOrderBean(savedOrder); // 💡 重要：連結主檔
			od.setProductId(item.getProduct().getProductId()); // 根據你 CartItem 的結構
			od.setProductName(item.getProduct().getProductName());
			od.setProductPrice(item.getProduct().getProductPrice());
			od.setQuantity(item.getQuantity());
			od.setSubtotal(item.getSubtotal());
			details.add(od);
		}

		// 儲存明細到資料庫
		odr.saveAll(details);
		System.out.println("✅ 已成功存入 " + details.size() + " 筆明細");

		// 清空購物車項目
		cir.deleteByCartId(cartId);

		// 建立並儲存金流紀錄
		String merchantTradeNo = "PET" + orderId + "T" + (System.currentTimeMillis() % 10000);
		OrderPaymentRecord record = new OrderPaymentRecord();
		record.setOrder(savedOrder);
		record.setMerchantTradeNo(merchantTradeNo);
		record.setPaymentAmt(finalAmount); // 金流紀錄也使用算好的最終金額
		record.setPaymentType(order.getOrderPayment());
		prr.save(record);

		// 呼叫綠界產出HTML跳轉Form (這時綠界只看 order 裡面的 OrderTotal，徹底與折扣解耦)
		return generateEcPayForm(savedOrder, merchantTradeNo);
	}

	// 用processCheckoutAndReturnDetail呼叫上面結帳的流程然後變成Map給Controller
	@Transactional
	public Map<String, Object> processCheckoutAndReturnDetail(Order order, Integer cartId,
			List<DiscountDetailDTO> appliedDiscounts) {
		// 先執行原本的結帳流程(回傳HTML String)
		String ecpayForm = processCheckout(order, cartId, appliedDiscounts);

		// 要丟回給前端的資料
		Map<String, Object> response = new java.util.HashMap<>();
		response.put("order", order);
		response.put("form", ecpayForm); // 這是綠界的跳轉表單

		return response;
	}

	private String generateEcPayForm(Order order, String merchantTradeNo) {
		Map<String, String> params = new TreeMap<>();
		params.put("MerchantID", MERCHANT_ID);
		params.put("MerchantTradeNo", merchantTradeNo);
		params.put("MerchantTradeDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
		params.put("PaymentType", "aio");
		// 綠界只負責收錢，直接拿訂單裡的最終總金額
		params.put("TotalAmount", String.valueOf(order.getOrderTotal().intValue()));
		params.put("TradeDesc", "PetLifeOrder");
		params.put("ItemName", "PetLifeProduct一批"); // 固定商品名稱
		params.put("ChoosePayment", "ALL");
		params.put("EncryptType", "1"); // 成功為1，失敗為0
		// params.put("ClientBackURL", "http://localhost:5173/shop"); // 取消訂單時的退款按鈕要用
		// 內網穿透工具(ngrok) 要正式測試要記得來改網址!!!
		params.put("ReturnURL", "https://enable-impeach-caress.ngrok-free.dev/api/payment/callback");
		params.put("ClientBackURL", "http://localhost:5173/checkoutsuccess");
		params.put("NeedExtraPaidInfo", "N");

		// 計算加密簽章CheckMacValue
		String checkMacValue = calculateCheckMacValue(params);
		params.put("CheckMacValue", checkMacValue);

		// 組合自動Submit的HTML表單
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

	private String calculateCheckMacValue(Map<String, String> params) {
		// 拼裝原始字串
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
			throw new RuntimeException("加密失敗：" + ex.getMessage());
		}
	}

	// 處理綠界回傳的通知，自動化同步訂單狀態
	@Transactional
	public void updateOrderPaymentStatus(String merchantTradeNo) {
		OrderPaymentRecord record = prr.findByMerchantTradeNo(merchantTradeNo).orElse(null);

		if (record != null) {
			// 找到對應的訂單，但不是從綠界，他們只會給我他們的交易編號
			Order order = record.getOrder();

			// 狀態改成 1
			order.setOrderStatus("1");
			or.save(order);

			System.out.println("訂單編號 " + order.getOrderId() + " 狀態更新成功");
		} else {
			System.out.println("找不到對應的金流紀錄：" + merchantTradeNo);
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
		result.put("usedPoint", order.getUsedPoint() != null ? order.getUsedPoint() : 0);

		// 抓取該訂單的所有明細
		List<OrderDetail> details = odr.findByOrderBean_OrderId(orderId);

		List<Map<String, Object>> itemsList = new ArrayList<>();
		if (details != null && !details.isEmpty()) {
			for (OrderDetail detail : details) {
				Map<String, Object> itemMap = new HashMap<>();

				itemMap.put("productName", detail.getProductName());
				itemMap.put("productPrice", detail.getProductPrice());
				itemMap.put("quantity", detail.getQuantity());
				itemMap.put("subtotal", detail.getSubtotal());
				itemMap.put("discount",
						detail.getDiscountAmount() != null ? detail.getDiscountAmount() : BigDecimal.ZERO);

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
			System.out.println("⚠️ 警告：資料庫中找不到訂單編號 " + orderId + " 的任何明細！");
		}

		result.put("items", itemsList);
		return result;
	}

	// 取得特定會員的所有訂單 (未刪除的)
	public List<Order> findByMemberId(Integer memberId) {
		return or.findByMemberIdAndIsDeletedFalseOrderByOrderDateDesc(memberId);
	}

	// 查詢單筆訂單
	public Order findById(Integer orderId) {
		// 回傳 Optional， .orElse(null)表示找不到就回傳 null
		return or.findById(orderId).orElse(null);
	}

	// 儲存/更新訂單（取消訂單後需要存回去）
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

			// 「已完成」時才增加總價1%點數
			if (order.getOrderTotal() != null) {
				int earned = calculateEarnedBonus(order.getOrderTotal());
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

		// 依日期降冪
		history.sort((a, b) -> ((LocalDateTime) b.get("date")).compareTo((LocalDateTime) a.get("date")));
		return history;
	}

	// 確認收貨並發放紅利點數
	@Transactional
	public void completeOrder(Integer orderId) {
		Order order = or.findById(orderId).orElseThrow(() -> new RuntimeException("找不到訂單"));
		if (!"已完成".equals(order.getOrderStatus()) && !"已取消".equals(order.getOrderStatus())) {
			order.setOrderStatus("已完成");
			int earnedPoints = calculateEarnedBonus(order.getOrderTotal());

			if (earnedPoints > 0) {
				memberRepository.addBonusPoints(order.getMemberId(), earnedPoints);
			}
			or.save(order);
		}
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

	// 計算扣除折扣與紅利後的最終金額
	private BigDecimal calculateFinalAmount(BigDecimal subTotal, List<DiscountDetailDTO> appliedDiscounts,
			Integer pointsUsed) {
		BigDecimal finalAmount = subTotal;

		// 扣除活動折扣
		if (appliedDiscounts != null && !appliedDiscounts.isEmpty()) {
			for (DiscountDetailDTO d : appliedDiscounts) {
				finalAmount = finalAmount.subtract(d.getAmount());
			}
		}

		// 扣除紅利點數 (假設 1 點 = 1 元)
		if (pointsUsed != null && pointsUsed > 0) {
			finalAmount = finalAmount.subtract(new BigDecimal(pointsUsed));
		}

		// 防呆機制：確保結帳金額最少為 0 元，不會出現負數
		if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
			finalAmount = BigDecimal.ZERO;
		}

		return finalAmount;
	}

	// 統一的紅利計算邏輯 (集中管理，未來好修改)
	public int calculateEarnedBonus(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			return 0;
		}
		// 目前設定為 1% (0.01) 回饋，無條件捨去
		return amount.multiply(new BigDecimal("0.01")).intValue();
	}
}