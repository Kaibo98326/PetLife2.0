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
import com.petlife.repository.CartItemRepository;
import com.petlife.repository.CartItemDTO;
import com.petlife.repository.CartCalculateResponseDTO;
import com.petlife.repository.DiscountDetailDTO;
import com.petlife.repository.OrderDetailRepository;
import com.petlife.repository.OrderPaymentRecordRepository;
import com.petlife.repository.OrderRepository;
import com.petlife.repository.ProductRepository;
import com.petlife.repository.DiscountRepository;
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

	@Transactional
	public String processCheckout(Order order, Integer cartId) {
		// 計算購物車總金額（原始金額）
		BigDecimal totalAmount = or.getCartTotal(order.getMemberId());
		// 預設最終付款金額 = 原始金額
		BigDecimal finalPaymentAmount = totalAmount;
		List<DiscountDetailDTO> appliedDiscountsForEcPay = new ArrayList<>();
		
		// 2026-05-14 修正：準備用來存折扣明細的暫存 List
		List<OrderDiscount> pendingOrderDiscounts = new ArrayList<>();

		// --- 2026-05-14 修正開始：將折扣計算移到 Order 存檔之前 ---
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
						Integer specificCatId = p.getCategories().stream().filter(c -> c.getCategoryType() != null && c.getCategoryType() == 1).map(com.petlife.model.Category::getCategoryId).findFirst().orElse(p.getCategories().iterator().next().getCategoryId());
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
					if (finalPaymentAmount.compareTo(BigDecimal.ZERO) < 0) finalPaymentAmount = BigDecimal.ZERO;
					System.out.println("後端活動修改：折扣總金額" + discountTotal + "，最終付款金額" + finalPaymentAmount);
				}

				if (calcResult.getAppliedDiscounts() != null && !calcResult.getAppliedDiscounts().isEmpty()) {
					appliedDiscountsForEcPay.addAll(calcResult.getAppliedDiscounts());
					for (DiscountDetailDTO detail : calcResult.getAppliedDiscounts()) {
						for (Discount discount : activeDiscounts) {
							if (discount.getDiscountName().equals(detail.getName())) {
								OrderDiscount od = new OrderDiscount();
								// 注意：這裡還沒有 OrderId，先暫存起來
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
			e.printStackTrace();
		}

		// 2026-05-14 修正：把算好的最終金額塞進訂單主檔中
		order.setOrderTotal(finalPaymentAmount);
		
		// 建立訂單，取得 orderId
		Order savedOrder = or.save(order);
		Integer orderId = savedOrder.getOrderId();

		// 2026-05-14 修正：取得 orderId 後，補上關聯並存入明細表
		if (!pendingOrderDiscounts.isEmpty()) {
			for (OrderDiscount od : pendingOrderDiscounts) {
				od.setOrderId(orderId);
			}
			orderDiscountService.saveAllOrderDiscounts(pendingOrderDiscounts);
		}
		// --- 2026-05-14 修正結束 ---
		
		// 轉移購物車資料到訂單明細 (此原生 SQL 預設將折扣填為 0)
				odr.transferCartToOrderDetails(orderId, order.getMemberId());

				// --- 2026-05-14 18:12 修正開始：將折扣金額寫回 OrderDetail 以便前端顯示 ---
				try {
					// 從資料庫抓出剛轉移過去的訂單明細
					List<OrderDetail> newlyCreatedDetails = odr.findByOrderBean_OrderId(orderId);
					if (newlyCreatedDetails != null && !newlyCreatedDetails.isEmpty() && !pendingOrderDiscounts.isEmpty()) {
						for (OrderDetail detail : newlyCreatedDetails) {
							for (OrderDiscount od : pendingOrderDiscounts) {
								// 依據商品 ID 比對，找到該商品的折扣紀錄
								if (detail.getProductId().equals(od.getProductId())) {
									// 1. 寫入折扣金額
									detail.setDiscountAmount(od.getDiscountAmount());
									
									// 2. 重新計算小計：原本的 (單價*數量) 減去 折扣金額
									BigDecimal newSubtotal = detail.getSubtotal().subtract(od.getDiscountAmount());
									// 確保小計不會變成負數
									detail.setSubtotal(newSubtotal.compareTo(BigDecimal.ZERO) > 0 ? newSubtotal : BigDecimal.ZERO);
									
									break; // 假設一個商品只配對一筆折扣，配對成功就換下一個明細
								}
							}
						}
						// 將更新好折扣與小計的明細，整批存回資料庫
						odr.saveAll(newlyCreatedDetails);
					}
				} catch (Exception e) {
					System.out.println("警告: 更新 OrderDetail 折扣金額失敗 " + e.getMessage());
					e.printStackTrace();
				}
				// --- 2026-05-14 18:12 修正結束 ---

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

		// 呼叫綠界產出HTML跳轉Form
		return generateEcPayForm(savedOrder, merchantTradeNo, finalPaymentAmount, appliedDiscountsForEcPay);
	}

	// 用processCheckoutAndReturnDetail呼叫上面結帳流程然後回傳Map給Controller
	@Transactional
	public Map<String, Object> processCheckoutAndReturnDetail(Order order, Integer cartId) {
		// 先執行原有的結帳流程(回傳HTML String)
		String ecpayForm = processCheckout(order, cartId);

		// 要回傳給前端的資料
		Map<String, Object> response = new java.util.HashMap<>();
		response.put("order", order);
		response.put("form", ecpayForm); // 這是綠界產生的跳轉表單

		return response;
	}

	// 活動修改：新增finalAmount參數，讓 ECPay 收到的是折扣後的最終金額
	private String generateEcPayForm(Order order, String merchantTradeNo, BigDecimal finalAmount, List<DiscountDetailDTO> appliedDiscounts) {
		// TreeMap會自動依字母A-Z排序參數
		Map<String, String> params = new TreeMap<>();
		params.put("MerchantID", MERCHANT_ID);
		params.put("MerchantTradeNo", merchantTradeNo);
		params.put("MerchantTradeDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
		params.put("PaymentType", "aio");
		// 活動修改：使用傳入的 finalAmount（折扣後金額），不再是 order.getOrderTotal()（原始金額）
		params.put("TotalAmount", String.valueOf(finalAmount.intValue()));
		params.put("TradeDesc", "PetLifeOrder");
		StringBuilder itemNameBuilder = new StringBuilder("PetLifeOrder");
		if (appliedDiscounts != null && !appliedDiscounts.isEmpty()) {
			for (DiscountDetailDTO d : appliedDiscounts) {
				itemNameBuilder.append("#").append(d.getName()).append(" -NT$").append(d.getAmount().intValue());
			}
		}
		params.put("ItemName", itemNameBuilder.toString());
		params.put("ChoosePayment", "ALL");
		params.put("EncryptType", "1"); // 1為SHA256加密
		params.put("ClientBackURL", "http://localhost:5173/shop"); // 接收訂單完成後的付款結果回傳
		// 內網穿透工具(ngrok) 要正式測試記得要來改網址!!!
		params.put("ReturnURL", "https://enable-impeach-caress.ngrok-free.dev/api/payment/callback");
//		params.put("OrderResultURL", "http://localhost:5173/checkoutsuccess");// 結帳完成後的頁面
		params.put("ClientBackURL", "http://localhost:5174/checkoutsuccess");
		params.put("NeedExtraPaidInfo", "N");
		
		// 計算產生簽章CheckMacValue
		String checkMacValue = calculateCheckMacValue(params);
		params.put("CheckMacValue", checkMacValue);

		// 組合產生Submit的HTML表單
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
	
	// 結帳成功要顯示資料明細
	public Map<String, Object> getOrderDetailWithItems(Integer orderId) {
	    Order order = or.findById(orderId).orElse(null);
	    if (order == null) return null;

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
	            itemMap.put("discount", detail.getDiscountAmount() != null ? detail.getDiscountAmount() : BigDecimal.ZERO);
	            
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
}
