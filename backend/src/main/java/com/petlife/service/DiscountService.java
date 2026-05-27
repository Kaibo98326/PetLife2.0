package com.petlife.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petlife.model.Category;
import com.petlife.model.Discount;
import com.petlife.model.DiscountCategory;
import com.petlife.model.DiscountProduct;
import com.petlife.model.DiscountType;
import com.petlife.model.Product;
import com.petlife.repository.CategoryRepository;
import com.petlife.repository.DiscountCategoryRepository;
import com.petlife.repository.DiscountProductRepository;
import com.petlife.repository.DiscountRepository;
import com.petlife.repository.DiscountTypeRepository;
import com.petlife.repository.ProductRepository;

// 引入訂單相關 Model 與 Repository 以及報表 DTO
import com.petlife.model.Order;
import com.petlife.model.OrderDetail;
import com.petlife.repository.OrderRepository;
import com.petlife.repository.DiscountAnalysisDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional // 確保事務完整性，失敗會自動回滾
public class DiscountService {

	@Autowired
	private DiscountRepository discountRepository;
	@Autowired
	private DiscountCategoryRepository discountCategoryRepository;
	@Autowired
	private DiscountProductRepository discountProductRepository;
	@Autowired
	private DiscountTypeRepository discountTypeRepository;

	// 這兩個 Repository，用來向資料庫要真實的商品與分類
	@Autowired
	private CategoryRepository categoryRepository;
	@Autowired
	private ProductRepository productRepository;

	// ✨ 新增：引入訂單 Repository 來抓取歷史訂單
	@Autowired
	private OrderRepository orderRepository;

	public List<DiscountType> getAllDiscountTypes() {
		return discountTypeRepository.findAll();
	}

	// ✨ 修改：還原為原來的 List<Discount> 全量查詢結構，確保前端本地進階篩選功能完好如初
	public List<Discount> getAllDiscounts() {
		return discountRepository.findAll();
	}

	// ✨ 修改：加入 tagCategoryId 參數，用來接收前端選取的標籤 ID
	public void saveDiscountWithDetails(Discount discount, List<Integer> categoryIds, List<Integer> mainProductIds,
			List<Integer> addonProductIds, List<Integer> addonCategoryIds, Integer tagCategoryId) { // ✨ 新增：接收前端傳來的標籤 ID

		if (discount.getDiscountId() != null) {
			// 先清除舊的關聯，避免更新/修改時一直重疊、重複新增
			discountCategoryRepository.deleteByDiscount_DiscountId(discount.getDiscountId());
			discountProductRepository.deleteByDiscount_DiscountId(discount.getDiscountId());
			
			// 清空實體記憶體中的舊關聯，防止快取快照髒資料
			discount.setDiscountCategories(new java.util.HashSet<>());
			discount.setDiscountProducts(new java.util.HashSet<>());
		}

		Discount savedDiscount = discountRepository.save(discount);

		// 處理主項目 (Main) - 嚴格依賴 scopeType
		if (discount.getScopeType() == 1) {
			if (categoryIds != null) {
				for (Integer catId : categoryIds) {
					Category category = categoryRepository.findById(catId).orElse(null);
					if (category != null) {
						DiscountCategory dc = new DiscountCategory(savedDiscount, category, "Main");
						discountCategoryRepository.save(dc);
					}
				}
			}
		} else if (discount.getScopeType() == 2) {
			if (mainProductIds != null) {
				for (Integer prodId : mainProductIds) {
					Product product = productRepository.findById(prodId).orElse(null);
					if (product != null) {
						DiscountProduct dp = new DiscountProduct(savedDiscount, product, "Main");
						discountProductRepository.save(dp);
					}
				}
			}
		}

		// ✨ 修改：處理副項目 (Addon) - 完全脫離 scopeType 限制！只要前端有傳，就獨立存入對應關聯表
		if (addonCategoryIds != null) {
			for (Integer catId : addonCategoryIds) {
				Category category = categoryRepository.findById(catId).orElse(null);
				if (category != null) {
					DiscountCategory dc = new DiscountCategory(savedDiscount, category, "Addon");
					discountCategoryRepository.save(dc);
				}
			}
		}
		if (addonProductIds != null) {
			for (Integer prodId : addonProductIds) {
				Product product = productRepository.findById(prodId).orElse(null);
				if (product != null) {
					DiscountProduct dp = new DiscountProduct(savedDiscount, product, "Addon");
					discountProductRepository.save(dp);
				}
			}
		}

		// ✨ 新增：處理活動標籤 (Tag) - 因為不管 scopeType 是 1 還是 2，都有可能掛載標籤，所以寫在 if-else 外面
		if (tagCategoryId != null) {
			Category tagCategory = categoryRepository.findById(tagCategoryId).orElse(null);
			if (tagCategory != null) {
				// 將這個分類的角色設定為 "Tag"
				DiscountCategory dc = new DiscountCategory(savedDiscount, tagCategory, "Tag");
				discountCategoryRepository.save(dc);
			}

		}
	}

	// 刪除
	public void deleteDiscount(Integer id) {
		discountRepository.deleteById(id);
	}

	/**
	 * 動態尋找該商品符合的最佳活動 支援「單品優先於分類」以及「時間區間自動過濾」
	 */
	public Discount findBestActiveDiscountForProduct(Integer productId, Integer categoryId) {

		// 1. 取得現在日期 (改用 Java 8 新版的 LocalDate)
		java.time.LocalDate today = java.time.LocalDate.now();

		// 2. 撈出所有活動
		List<Discount> allDiscounts = discountRepository.findAll();

		// 3. 過濾出「進行中」的活動列表
		List<Discount> activeDiscounts = allDiscounts.stream().filter(d -> "active".equals(d.getStatus()))
				.filter(d -> d.getStartDate() != null && d.getEndDate() != null)
				// isBefore() 和 isAfter() 來比較 LocalDate
				// !today.isBefore(startDate) 代表「今天 >= 開始日」
				// !today.isAfter(endDate) 代表「今天 <= 結束日」
				.filter(d -> !today.isBefore(d.getStartDate()) && !today.isAfter(d.getEndDate()))
				.collect(Collectors.toList());

		// --- 第一優先權：檢查「指定單品 (Scope 2)」 ---
		for (Discount d : activeDiscounts) {
			if (d.getScopeType() == 2) {
				// 檢查該活動的商品關聯清單中是否包含此商品，且身分為 Main
				boolean isMatch = d.getDiscountProducts().stream().anyMatch(
						dp -> dp.getProduct().getProductId().equals(productId) && "Main".equals(dp.getProductRole()));
				if (isMatch)
					return d;
			}
		}

		// --- 第二優先權：檢查「指定分類 (Scope 1)」 ---
		for (Discount d : activeDiscounts) {
			if (d.getScopeType() == 1) {
				// 檢查該活動的分類關聯清單中是否包含此分類，且身分為 Main
				boolean isMatch = d.getDiscountCategories().stream()
						.anyMatch(dc -> dc.getCategory().getCategoryId().equals(categoryId)
								&& "Main".equals(dc.getCategoryRole()));
				if (isMatch)
					return d;
			}
		}

		// 4. 若皆無符合活動，回傳 null
		return null;
	}

	// ✨ 新增：動態回推活動成效報表演算法 (無痛物理隔離版，不改 DB 欄位)
	public List<DiscountAnalysisDTO> getDiscountAnalysis(Integer discountId) {
		// 1. 抓出活動劇本
		Discount discount = discountRepository.findById(discountId).orElse(null);
		if (discount == null || discount.getStartDate() == null || discount.getEndDate() == null) {
			return new ArrayList<>();
		}

		// 2. 撈出所有未軟刪除的歷史訂單
		List<Order> allValidOrders = orderRepository.findByIsDeletedFalseOrderByOrderDateDesc();
		Map<String, DiscountAnalysisDTO> analysisMap = new HashMap<>();

		for (Order order : allValidOrders) {
			// 防呆：排除已取消的訂單
			if ("已取消".equals(order.getOrderStatus()))
				continue;

			// 檢查訂單時間是否落在活動期間內
			LocalDate orderDate = order.getOrderDate().toLocalDate();
			if (orderDate.isBefore(discount.getStartDate()) || orderDate.isAfter(discount.getEndDate())) {
				continue;
			}

			// 3. 交叉比對該筆訂單的所有明細
			for (OrderDetail detail : order.getDetails()) {
				// 必須要有真實折抵金額才算數 (防呆：折抵大於 0)
				if (detail.getDiscountAmount() == null || detail.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}

				boolean isMatch = false;

				if (discount.getScopeType() == 2) {
					// 指定單品：檢查這筆明細的 productId 是否在活動的指定商品清單中
					isMatch = discount.getDiscountProducts().stream()
							.anyMatch(dp -> dp.getProduct().getProductId().equals(detail.getProductId()));
				} else if (discount.getScopeType() == 1) {
					// 指定分類：查出真實商品，並打開其 Categories 陣列檢查是否包含活動的指定分類
					Product product = productRepository.findById(detail.getProductId()).orElse(null);
					if (product != null) {
						List<Integer> productCategoryIds = product.getCategories().stream().map(Category::getCategoryId)
								.collect(Collectors.toList());

						isMatch = discount.getDiscountCategories().stream()
								.anyMatch(dc -> productCategoryIds.contains(dc.getCategory().getCategoryId()));
					}
				}

				// 4. 動態加總 (Group By ProductName 封裝至 DTO)
				if (isMatch) {
					String pName = detail.getProductName();
					DiscountAnalysisDTO dto = analysisMap.getOrDefault(pName,
							new DiscountAnalysisDTO(pName, BigDecimal.ZERO, 0));
					dto.setDiscountAmount(dto.getDiscountAmount().add(detail.getDiscountAmount()));
					dto.setQuantity(dto.getQuantity() + detail.getQuantity());
					analysisMap.put(pName, dto);
				}
			}
		}

		return new ArrayList<>(analysisMap.values());
	}
}