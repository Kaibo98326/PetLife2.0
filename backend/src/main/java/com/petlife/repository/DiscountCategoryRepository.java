package com.petlife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.petlife.model.DiscountCategory;

import java.util.List;

@Repository
public interface DiscountCategoryRepository extends JpaRepository<DiscountCategory, Integer> {
	//JPA 進入 DiscountCategory 表格，找到 discount 這個物件屬性，再進一步比對該物件內部的 discountId
	// 找出某個活動所屬的所有分類綁定
   List<DiscountCategory> findByDiscount_DiscountId(Integer discountId);
    
    
    //一次性清空該活動在 DiscountCategory 表格中的所有紀錄
//修改活動並重新選擇分類後，保險的方法是先刪掉舊的所有關聯，再把新的關聯存進去，這能避免複雜的資料比對邏輯
    // 刪除特定活動的所有分類綁定 (修改活動時會用到)
//所有的 delete 或 update 自定義方法都必須加上@Modifying，否則程式會拋出異常，因為預設的方法只處理讀取或單筆存取
   @Modifying    // 告訴 JPA 這是一個會變動資料的動作
   @Transactional //確保刪除動作在一個交易中完成，若失敗會自動回滾
   void deleteByDiscount_DiscountId(Integer discountId);
  //  _ 是一個「路徑導航」告訴JPA請從 DiscountCategory 導向discount實體，再導向 discountId 欄位,
    //這讓你能直接用 ID 進行過濾，而不需要傳入整個 Discount 物件
}
