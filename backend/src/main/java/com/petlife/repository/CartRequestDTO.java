package com.petlife.repository;

import java.util.List;

/**
 * 接收前端計算請求的 DTO    ---->活動新增
 */
public class CartRequestDTO {
    private List<CartItemDTO> cartItems;

    public CartRequestDTO() {}

    public List<CartItemDTO> getCartItems() { return cartItems; }
    public void setCartItems(List<CartItemDTO> cartItems) { this.cartItems = cartItems; }
}