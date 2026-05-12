package com.petlife.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.petlife.model.CartItem;
import com.petlife.model.Discount;
import com.petlife.service.CartService;
import com.petlife.service.MemberService;
import com.petlife.model.Member;

//活動匯入，因應即時折扣計算
import com.petlife.service.DiscountEngine; //  折扣分類核心
//專門用來裝單一個購物車商品的核心資訊（如：商品ID、分類ID、價格、數量）
import com.petlife.repository.CartItemDTO;  
//處理CartItemDTO，當前端按下結帳或計算時，就是把這個list傳給後端，後端才知道購物車裡到底有哪些東西要算錢
import com.petlife.repository.CartRequestDTO;
import com.petlife.repository.DiscountRepository;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;
    
    @Autowired
    private MemberService memberService;

 // 折扣相關
    @Autowired
    private DiscountEngine discountEngine;
    //活動
    @Autowired
    private DiscountRepository discountRepository;
    
    // 取得購物車清單
    @GetMapping("/{memberId}")
    public ResponseEntity<List<CartItem>> getMyCart(@PathVariable Integer memberId) {
        return ResponseEntity.ok(cartService.getCartItems(memberId));
    }

    // 取得購物車商品總件數
    @GetMapping("/count/{memberId}")
    public ResponseEntity<Integer> getCartCount(@PathVariable Integer memberId) {
        Integer count = cartService.getCartTotalQuantity(memberId);
        return ResponseEntity.ok(count);
    }

    // 加入購物車
    @PostMapping("/add/{memberId}")
    public ResponseEntity<String> addItem(@PathVariable Integer memberId, @RequestBody CartItem item) {
        try {
            cartService.addToCart(memberId, item);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // 刪除特定品項
    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<String> removeItem(@PathVariable Integer itemId) {
        cartService.removeItemFromCart(itemId);
        return ResponseEntity.ok("removed");
    }

    // 更新數量(取代原本的/update)
    // + - 按鈕
    @PutMapping("/update/{itemId}")
    public ResponseEntity<String> updateQuantity(
            @PathVariable Integer itemId, 
            @RequestParam Integer quantity) {
        try {
            cartService.updateCartItemQuantity(itemId, quantity);
            return ResponseEntity.ok("updated");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    // 提供給 CheckoutView.vue 呼叫的接口
    @GetMapping("/member/info/{id}")
    public ResponseEntity<?> getMemberInfoForCheckout(@PathVariable Integer id) {
    	Member member = memberService.findById(id).orElse(null);
        if (member != null) {
            return ResponseEntity.ok(member); // 回傳會員物件
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("找不到會員");
    }
    
    
    
 // -即時折扣計算 API
    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateCartDiscount(@RequestBody CartRequestDTO request) {
        List<CartItemDTO> cartItems = request.getCartItems();

        // 1. 計算原價總額 (Original Total)
        BigDecimal originalTotal = cartItems.stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. 取得進行中的活動
        java.time.LocalDate today = java.time.LocalDate.now();
        List<Discount> activeDiscounts = discountRepository.findAll().stream()
                .filter(d -> "active".equals(d.getStatus()))
                .filter(d -> d.getStartDate() != null && d.getEndDate() != null)
                .filter(d -> !today.isBefore(d.getStartDate()) && !today.isAfter(d.getEndDate()))
                .collect(Collectors.toList());

        // 3. 呼叫折扣引擎執行計算
        BigDecimal discountAmount = discountEngine.executeDiscount(cartItems, activeDiscounts);

        // 4. 計算應付總額 (Final Amount)
        BigDecimal finalAmount = originalTotal.subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;

        // 5. 回傳結果給前端
        Map<String, Object> result = new HashMap<>();
        result.put("originalTotal", originalTotal);
        result.put("discountAmount", discountAmount);
        result.put("finalAmount", finalAmount);

        return ResponseEntity.ok(result);
    }
}