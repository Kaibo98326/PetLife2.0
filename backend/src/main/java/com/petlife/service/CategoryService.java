package com.petlife.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petlife.model.Category;
import com.petlife.repository.CategoryRepository;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

//===== 查詢所有分類 =========================================================================================
    @Transactional(readOnly = true) // 查詢操作設定為唯讀，優化效能
    public List<Category> getAllCategory() {
        return categoryRepository.findAll();
    }

//===== 查詢單筆分類 =========================================================================================
    @Transactional(readOnly = true)	// 查詢操作可設定為唯讀，優化效能
    public Category getCategoryById(Integer id) {
        return categoryRepository.findById(id).orElse(null);
    }

//===== 新增分類 ============================================================================================
    public Category addCategory(Category category) {
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        return categoryRepository.save(category);
    }

//===== 修改分類 ============================================================================================
    public Category updateCategory(Category category) {
        return categoryRepository.save(category);
    }

  //===== 刪除分類 ============================================================================================
    public void deleteCategory(Integer id) {
        //  新增/修改：系統核心容器防呆機制
        // 若傳入的 ID 為 3 (🔥優惠活動)，拋出例外，禁止刪除此保留項目
        if (id != null && id == 3) {
            throw new IllegalArgumentException("此為系統保留之核心容器，禁止刪除！");
        }
        categoryRepository.deleteById(id);
    }
    
 // 專供消費者前台取得分類清單 (隱藏空標籤與未生效標籤)
    @Transactional(readOnly = true)
    public List<Category> getFrontEndCategories() {
        return categoryRepository.findFrontEndCategories();
    }
}