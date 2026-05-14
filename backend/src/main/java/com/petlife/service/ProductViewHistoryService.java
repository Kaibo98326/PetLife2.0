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
import java.util.List;
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
     */
    public List<Product> getRecentViews(Integer memberId) {
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isPresent()) {
            List<ProductViewHistory> histories = historyRepository.findByMemberOrderByViewTimeDesc(memberOpt.get());
            
            // 取前 10 筆並轉換為 Product 物件
            return histories.stream()
                    .limit(10)
                    .map(ProductViewHistory::getProduct)
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
