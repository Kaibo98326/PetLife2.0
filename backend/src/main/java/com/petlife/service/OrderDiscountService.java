package com.petlife.service;

import com.petlife.model.OrderDiscount;
import com.petlife.model.Discount;
import com.petlife.repository.OrderDiscountRepository;
import com.petlife.repository.OrderDiscountSummaryDTO;
import com.petlife.repository.DiscountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderDiscountService {

	@Autowired
	private OrderDiscountRepository orderDiscountRepository;

	// ✨ 新增：注入 DiscountRepository 與 DiscountTemplateHelper 以便動態生成樣板字串
	@Autowired
	private DiscountRepository discountRepository;

	@Autowired
	private DiscountTemplateHelper templateHelper;

	// 1. 批量儲存訂單折扣紀錄 (結帳時呼叫)
	public List<OrderDiscount> saveAllOrderDiscounts(List<OrderDiscount> discounts) {
		return orderDiscountRepository.saveAll(discounts);
	}

	// 2. 儲存單筆訂單折扣紀錄
	public OrderDiscount saveOrderDiscount(OrderDiscount discount) {
		return orderDiscountRepository.save(discount);
	}

	// 3. 根據訂單ID查詢折扣明細 (後台訂單管理 / 圖1活動成效查看用)
	public List<OrderDiscount> getDiscountsByOrderId(Integer orderId) {
		return orderDiscountRepository.findByOrderId(orderId);
	}

	// 透過活動 ID 查詢該活動被哪些訂單使用 (給後台明細按鈕用)
	public List<OrderDiscount> getDiscountsByDiscountId(Integer discountId) {
		return orderDiscountRepository.findByDiscountId(discountId);
	}

	// 活動修改：回傳含活動名稱的摘要，供結帳成功頁顯示折抵明細（不需重新計算）
	public List<OrderDiscountSummaryDTO> getDiscountSummaryByOrderId(Integer orderId) {
		// ✨ 修改：為了不改動資料庫結構，改為「動態生成樣板字串」。
		// 先撈出該訂單的所有折扣紀錄
		List<OrderDiscount> orderDiscounts = orderDiscountRepository.findByOrderId(orderId);
		List<OrderDiscountSummaryDTO> summaryList = new ArrayList<>();

		for (OrderDiscount od : orderDiscounts) {
			// 透過 discountId 找出對應的活動實體
			Discount discount = discountRepository.findById(od.getDiscountId()).orElse(null);
			if (discount != null) {
				// ✨ 核心魔法：動態組裝「單一食品 享 85折」等完整行銷樣板，取代原本單調的活動名稱
				String templateText = templateHelper.generateDiscountDetailText(discount);
				summaryList.add(new OrderDiscountSummaryDTO(templateText, od.getDiscountAmount()));
			}
		}
		return summaryList;
	}

	// 呼叫新的 JOIN 查詢方法，提供給前端活動儀表板使用
	public List<OrderDiscountRepository.DiscountUsageProjection> getDiscountUsageDetails(Integer discountId) {
		return orderDiscountRepository.findDiscountUsageDetails(discountId);
	}
}