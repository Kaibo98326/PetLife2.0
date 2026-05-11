package com.petlife.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.petlife.dto.DiscountRequestDTO;
import com.petlife.model.Discount;
import com.petlife.model.DiscountType;
import com.petlife.service.DiscountService;



@RestController
@RequestMapping("/api/discounts") // 設定 API 的基礎網址
@CrossOrigin // 允許 Vue 跨網域發送請求 (實務上視你的環境設定而定)
public class DiscountController {

    @Autowired
    private DiscountService discountService;

    //這支 API：讓 Vue 取得所有的「折扣活動列表」
    @GetMapping
    public ResponseEntity<List<Discount>> getAllDiscounts() {
        try {
            List<Discount> discounts = discountService.getAllDiscounts();
            return ResponseEntity.ok(discounts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
  

    //  新增這支 API：讓 Vue 來拿折扣類型選單
    @GetMapping("/types")
    public ResponseEntity<List<DiscountType>> getDiscountTypes() {
        try {
            List<DiscountType> types = discountService.getAllDiscountTypes();
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 接收前端 Vue 傳來的新增活動請求
     */
    @PostMapping("/save")
    public ResponseEntity<String> saveDiscount(@RequestBody DiscountRequestDTO request) {
        try {
            // ✨ 修正：把第 5 個參數 (addonCategoryIds) 傳遞給 Service
            discountService.saveDiscountWithDetails(
                request.getDiscount(), 
                request.getCategoryIds(), 
                request.getMainProductIds(), 
                request.getAddonProductIds(),
                request.getAddonCategoryIds() 
            );
            
            return ResponseEntity.ok("活動儲存成功！");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("活動儲存失敗：" + e.getMessage());
        }
    }
}