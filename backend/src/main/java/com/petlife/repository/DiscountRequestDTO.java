package com.petlife.repository;


import lombok.Getter;
import lombok.Setter;
import java.util.List;

import com.petlife.model.Discount;

@Getter
@Setter
public class DiscountRequestDTO {
    
    // 包含前端填寫的活動基本資訊 (名稱、時間、折扣值等)
    private Discount discount;
    
    // 勾選的分類 ID 清單 (當 scope_type = 1 時使用)
    private List<Integer> categoryIds;
    
    // 勾選的主商品 ID 清單 (當 scope_type = 2 時使用)
    private List<Integer> mainProductIds;
    
    // 勾選的加購品/贈品 ID 清單 (當 scope_type = 2 且有加購品時使用)
    private List<Integer> addonProductIds;
    
 // 補上這個新欄位來接收前端的「加購分類」
    private List<Integer> addonCategoryIds;

 // ✨ 新增：接收前端傳來的「活動標籤 ID」
    private Integer tagCategoryId;

    public List<Integer> getAddonCategoryIds() {
        return addonCategoryIds;
    }
    public void setAddonCategoryIds(List<Integer> addonCategoryIds) {
        this.addonCategoryIds = addonCategoryIds;
    }

    // ✨ 新增：標籤的 Getter 與 Setter
    public Integer getTagCategoryId() {
        return tagCategoryId;
    }
    public void setTagCategoryId(Integer tagCategoryId) {
        this.tagCategoryId = tagCategoryId;
    }
    
    
    
}