package com.petlife.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.petlife.model.CartItem;
import com.petlife.service.CartService;
import com.petlife.service.MemberService;
import com.petlife.model.Member;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")  // 跨域
public class CartController {

    @Autowired
    private CartService cartService;
    
    @Autowired
    private MemberService memberService;

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
    
    // 串金流用
    
}