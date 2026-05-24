package com.petlife.service;

import com.petlife.model.Member;
import com.petlife.model.Product;
import com.petlife.model.ProductViewHistory;
import com.petlife.repository.MemberRepository;
import com.petlife.repository.ProductRepository;
import com.petlife.repository.ProductViewHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductViewHistoryService {

    @Autowired
    private ProductViewHistoryRepository historyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    /**
     * 記錄商品瀏覽
     */
    public void recordView(Integer memberId, Integer productId) {
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        Optional<Product> productOpt = productRepository.findById(productId);

        if (memberOpt.isPresent() && productOpt.isPresent()) {
            Member member = memberOpt.get();
            Product product = productOpt.get();

            // 檢查是否已有紀錄
            Optional<ProductViewHistory> existing = historyRepository.findByMemberAndProduct(member, product);

            if (existing.isPresent()) {
                // 更新時間
                ProductViewHistory history = existing.get();
                history.setViewTime(LocalDateTime.now());
                historyRepository.save(history);
            } else {
                // 新增紀錄
                ProductViewHistory history = ProductViewHistory.builder()
                        .member(member)
                        .product(product)
                        .viewTime(LocalDateTime.now())
                        .build();
                historyRepository.save(history);
            }
        }
    }

    /**
     * 取得最近瀏覽紀錄 (前 10 筆)
     * 回傳 Map 而非 Product 實體，避免 Hibernate Proxy 序列化問題
     */
    public List<Map<String, Object>> getRecentViews(Integer memberId) {
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isPresent()) {
            List<ProductViewHistory> histories = historyRepository.findByMemberOrderByViewTimeDesc(memberOpt.get());

            // 取前 10 筆，手動轉換為前端需要的欄位 Map
            // 加入 try-catch 避免商品已被刪除 (EntityNotFoundException) 導致整個 API 500 錯誤
            return histories.stream()
                    .map(h -> {
                        try {
                            Product p = h.getProduct();
                            // 讀取屬性觸發 Proxy 初始化，若商品不存在會拋出 EntityNotFoundException
                            Map<String, Object> map = new HashMap<>();
                            map.put("productId", p.getProductId());
                            map.put("productName", p.getProductName());
                            map.put("productPrice", p.getProductPrice());
                            map.put("productImage", p.getProductImage());
                            return map;
                        } catch (Exception e) {
                            // 忽略已被刪除或無法載入的商品紀錄
                            return null;
                        }
                    })
                    .filter(map -> map != null)
                    .limit(10)
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
