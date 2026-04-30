package com.petlife.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.petlife.model.Category;
import com.petlife.service.CategoryService;

@RestController
@RequestMapping("/api/categories")
public class InnerCategoryController {

    @Autowired
    private CategoryService categoryService;

    // 取得所有分類 (回傳 JSON 陣列)
    @GetMapping
    public ResponseEntity<List<Category>> getAll() {
        return ResponseEntity.ok(categoryService.getAllCategory());
    }

    // 取得單一分類 (Vue 的修改頁面可以根據 ID 查資料)
    @GetMapping("/{id}")
    public ResponseEntity<Category> getOne(@PathVariable Integer id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    // 新增分類
    @PostMapping
    public ResponseEntity<String> insert(@RequestBody Category category) {
        categoryService.addCategory(category);
        return ResponseEntity.ok("success");
    }

    // 更新分類
    @PutMapping("/{id}") // 改用 PUT 代表更新
    public ResponseEntity<String> update(@PathVariable Integer id, @RequestBody Category category) {
        category.setCategoryId(id);
        categoryService.updateCategory(category);
        return ResponseEntity.ok("success");
    }

    // 刪除分類
    @DeleteMapping("/{id}") // 改用 DELETE 代表刪除
    public ResponseEntity<String> delete(@PathVariable Integer id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok("success");
    }
}