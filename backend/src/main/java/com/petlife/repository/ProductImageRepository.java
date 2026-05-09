package com.petlife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.petlife.model.ProductImage;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {
    // 可以在這裡新增根據 productId 刪除所有圖片的方法
    void deleteByProduct_ProductId(Integer productId);
}
