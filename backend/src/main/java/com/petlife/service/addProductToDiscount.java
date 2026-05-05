package com.petlife.service;

@service
public void addProductToDiscount(Integer discountId, Integer productId) {
    // 1. 找到你的活動
    Discount discount = discountRepo.findById(discountId).get();
    
    // 2. ✨ 使用組員的 Repository 找到商品物件 ✨
    // 你需要 Product 類別才能承接這個商品資料
    Product product = productRepo.findById(productId).get();
    
    // 3. 建立關聯並存檔
    DiscountProduct mapping = new DiscountProduct(discount, product, "Main");
    discountProductRepo.save(mapping);
}