package com.petlife.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petlife.model.Heart;
import com.petlife.model.Product;
import com.petlife.repository.HeartRepository;
import com.petlife.repository.ProductRepository;

@Service
@Transactional
public class HeartService {

	@Autowired
	private HeartRepository hp;

	@Autowired
	private ProductRepository pr;
	
	@Autowired
	private ProductService productService;

	// 切換收藏狀態
	public String toggleHeart(Integer memberId, Integer productId) {
		// 用Repository找出第一筆資料
		List<Heart> hearts = hp.findByMemberIdAndProductId(memberId, productId);

		if (!hearts.isEmpty()) {
			// 如果已收藏，直接刪除該物件
			hp.deleteAll(hearts);
			return "已取消收藏";
		} else {
			// 找出商品當前價格，若無商品則拋出
			Product product = pr.findById(productId)
					.orElseThrow(() -> new IllegalArgumentException("商品編號 " + productId + " 不存在"));

			Heart newHeart = new Heart();
			newHeart.setMemberId(memberId);
			newHeart.setProductId(productId);
			// 存入目前價格作當基準
			newHeart.setTrackedPrice(product.getProductPrice());

			hp.save(newHeart);
			return "已加入收藏";
		}
	}

	// 獲取追蹤清單並進行即時比價
	public List<Heart> getWatchListWithComparison(Integer memberId) {
		List<Heart> heartList = hp.findByMemberId(memberId);

		for (Heart heart : heartList) {
			pr.findById(heart.getProductId()).ifPresent(product -> {
				BigDecimal nowPrice = product.getProductPrice();
				BigDecimal oldPrice = heart.getTrackedPrice();

				heart.setCurrentPrice(nowPrice);
				// compareTo != 0 代表價格有變動
				if (oldPrice != null && nowPrice != null) {
					heart.setIsPriceChange(nowPrice.compareTo(oldPrice) != 0);
				}
			});
		}
		return heartList;
	}

	// 刪除特定收藏
	public void removeHeart(Integer memberId, Integer productId) {
		hp.deleteByMemberIdAndProductId(memberId, productId);
	}
	
	public List<Heart> getMemberHearts(Integer memberId) {
        // 拿收藏紀錄
        List<Heart> hearts = hp.findByMemberId(memberId);

        // 產品資料
        for (Heart heart : hearts) {
            Product p = productService.getProductById(heart.getProductId());
            
            if (p != null) {
                if (p.getCategories() != null && !p.getCategories().isEmpty()) {
                    String names = p.getCategories().stream()
                            .map(cat -> cat.getCategoryName())
                            .collect(java.util.stream.Collectors.joining(", "));
                    p.setCategoryName(names);
                }
                heart.setProduct(p);
            }
        }
        // 過濾空的product
        return hearts.stream()
                     .filter(h -> h.getProduct() != null)
                     .collect(java.util.stream.Collectors.toList());
    	}
	}