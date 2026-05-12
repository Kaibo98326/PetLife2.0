package com.petlife.repository;

import com.petlife.model.Member;
import com.petlife.model.Product;
import com.petlife.model.ProductViewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductViewHistoryRepository extends JpaRepository<ProductViewHistory, Integer> {
    
    // 找出該會員的瀏覽紀錄，按時間降序排列
    List<ProductViewHistory> findByMemberOrderByViewTimeDesc(Member member);
    
    // 找出特定的會員與商品紀錄，用於更新時間
    Optional<ProductViewHistory> findByMemberAndProduct(Member member, Product product);
    
    // 限制數量的查詢可以透過 Service 處理內容，或使用 Pageable
}
