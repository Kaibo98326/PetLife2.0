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
import com.petlife.model.OrderPaymentRecord;
import com.petlife.repository.CartItemRepository;
import com.petlife.repository.OrderDetailRepository;
import com.petlife.repository.OrderPaymentRecordRepository;
import com.petlife.repository.OrderRepository;

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

	// 綠界測試環境常數設定
	private final String MERCHANT_ID = "3002607";
	private final String HASH_KEY = "pwFHCqoQZGmho4w6";
	private final String HASH_IV = "EkRm7iFT261dpevs";
	private final String SERVICE_URL = "https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5";

	@Transactional
	public String processCheckout(Order order, Integer cartId) {
		// 計算購物車總金額
		BigDecimal totalAmount = or.getCartTotal(order.getMemberId());
		order.setOrderTotal(totalAmount);
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
		record.setPaymentAmt(totalAmount);
		record.setPaymentType(order.getOrderPayment());
		prr.save(record);

		// 呼叫綠界產出HTML跳轉Form
		return generateEcPayForm(savedOrder, merchantTradeNo);
	}

	// 用processCheckoutAndReturnDetail呼叫上面結帳的流程然後變成Map給Controller
	@Transactional
	public Map<String, Object> processCheckoutAndReturnDetail(Order order, Integer cartId) {
		// 先執行原本的結帳流程(回傳HTML String)
		String ecpayForm = processCheckout(order, cartId);

		// 要丟回給前端的資料
		Map<String, Object> response = new java.util.HashMap<>();
		response.put("order", order);
		response.put("form", ecpayForm); // 這是綠界的跳轉表單

		return response;
	}

	private String generateEcPayForm(Order order, String merchantTradeNo) {
		// TreeMap自動按字母A-Z排序參數
		Map<String, String> params = new TreeMap<>();
		params.put("MerchantID", MERCHANT_ID);
		params.put("MerchantTradeNo", merchantTradeNo);
		params.put("MerchantTradeDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
		params.put("PaymentType", "aio");
		params.put("TotalAmount", String.valueOf(order.getOrderTotal().intValue()));
		params.put("TradeDesc", "PetLifeOrder");
		params.put("ItemName", "PetLifeProduct一批");
		params.put("ChoosePayment", "ALL");
		params.put("EncryptType", "1"); // 成功為1，失敗為0
//		params.put("ClientBackURL", "http://localhost:5173/shop"); // 取消訂單時的退款按鈕要用
		// 內網穿透工具(ngrok) 要正式測試要記得來改網址!!!
		params.put("ReturnURL", "https://enable-impeach-caress.ngrok-free.dev/api/payment/callback");
//		params.put("OrderResultURL", "http://localhost:5173/checkoutsuccess");// 結帳完導回的頁面
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
//	            itemMap.put("discount", detail.getDiscount() != null ? detail.getDiscount() : 0);

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
}