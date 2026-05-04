package com.petlife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.petlife.model.CartItem;

import java.util.List;

@Transactional
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    // 根據購物車ID查詢所有項目
    List<CartItem> findByCartId(Integer cartId);
    
    // 購物車有這個商品就更新數量，沒有的話就新增商品進去
    CartItem findByCartIdAndProductId(Integer cartId, Integer productId);
    
    // 結帳清空購物車
    void deleteByCartId(Integer cartId); 
   
}
