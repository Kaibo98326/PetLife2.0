package com.petlife.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import com.petlife.model.Discount;
import com.petlife.model.DiscountType;
import com.petlife.repository.DiscountRequestDTO;
// ✨ 新增：引入給前端畫圓餅圖專用的報表 DTO
import com.petlife.repository.DiscountAnalysisDTO;
import com.petlife.service.DiscountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;



@RestController
@RequestMapping("/api/discounts") // 設定 API 的基礎網址
@CrossOrigin // 允許 Vue 跨網域發送請求 (實務上視你的環境設定而定)
public class DiscountController {

    @Autowired
    private DiscountService discountService;

    @GetMapping
    public ResponseEntity<List<Discount>> getAllDiscounts() {
        try {
            List<Discount> discounts = discountService.getAllDiscounts();
            return ResponseEntity.ok(discounts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    //  新增這支 API：讓 Vue 來拿折扣類型選單(useDiscount.js)
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
            // 第 5 個參數 (addonCategoryIds) 傳遞給 Service
            // ✨ 修改：補上第 6 個參數 request.getTagCategoryId()，對應 Service 新增的接收標籤 ID 邏輯
            discountService.saveDiscountWithDetails(
                request.getDiscount(), 
                request.getCategoryIds(), 
                request.getMainProductIds(), 
                request.getAddonProductIds(),
                request.getAddonCategoryIds(),
                request.getTagCategoryId() // ✨ 新增：將前端傳來的標籤 ID 傳入 Service
            );
            
            return ResponseEntity.ok("活動儲存成功！");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("活動儲存失敗：" + e.getMessage());
        }
    }
    
    
    /**
     * 接收前端 Vue 傳來的修改活動請求
     */
    @PutMapping("/{id}")
    public ResponseEntity<String> updateDiscount(@PathVariable Integer id, @RequestBody DiscountRequestDTO request) {
        try {
            // ✨ 關鍵：把網址上的 ID 塞入 discount 物件中
            // 這樣 Spring Data JPA 存檔時，才會知道這是「更新舊資料」而不是「新增一筆」
            request.getDiscount().setDiscountId(id);
            
            // ✨ 修改：補上第 6 個參數 request.getTagCategoryId()，確保修改時也能正確更新標籤關聯
            discountService.saveDiscountWithDetails(
                request.getDiscount(), 
                request.getCategoryIds(), 
                request.getMainProductIds(), 
                request.getAddonProductIds(),
                request.getAddonCategoryIds(),
                request.getTagCategoryId() // ✨ 新增：將標籤 ID 傳入 Service 處理
            );
            
            return ResponseEntity.ok("活動修改成功！");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("活動修改失敗：" + e.getMessage());
        }
    }
    /**
     * 接收前端 Vue 傳來的刪除活動請求 (硬刪除備用)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDiscount(@PathVariable Integer id) {
        try {
            discountService.deleteDiscount(id);
            return ResponseEntity.ok("活動刪除成功！");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("活動刪除失敗：" + e.getMessage());
        }
    }

    // ✨ 新增：提供前端 Vue 儀表板圓餅圖的成效分析 API
    @GetMapping("/analysis/{id}")
    public ResponseEntity<List<DiscountAnalysisDTO>> getDiscountAnalysis(@PathVariable Integer id) {
        try {
            // 呼叫 Service 的動態回推演算法，拿回分群加總好的資料
            List<DiscountAnalysisDTO> analysisData = discountService.getDiscountAnalysis(id);
            return ResponseEntity.ok(analysisData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}