package com.petlife.repository;

import java.util.List;

import com.petlife.model.Order;

public class CheckoutRequsetDTO {
	
	private Order order;
	
	private List<DiscountDetailDTO> appliedDiscounts;
	
	private List<CartItemDTO> cartItems;
	
	public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public List<DiscountDetailDTO> getAppliedDiscounts() {
        return appliedDiscounts;
    }

    public void setAppliedDiscounts(List<DiscountDetailDTO> appliedDiscounts) {
        this.appliedDiscounts = appliedDiscounts;
    }
    
    public List<CartItemDTO> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItemDTO> cartItems) {
        this.cartItems = cartItems;
    }
}
