package com.petlife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.petlife.model.DiscountProduct;
import java.util.List;

@Repository
public interface DiscountProductRepository extends JpaRepository<DiscountProduct, Integer> {

    // ✨ 方便查詢：找出某個活動所屬的所有商品綁定
    List<DiscountProduct> findByDiscount_DiscountId(Integer discountId);

    // 刪除特定活動的所有商品綁定
    void deleteByDiscount_DiscountId(Integer discountId);
}