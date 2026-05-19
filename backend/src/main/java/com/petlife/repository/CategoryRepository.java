package com.petlife.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.petlife.model.Category;


@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
	
	// 專供消費者前台使用的分類查詢 (加入 c.categoryId = 3 保留大標題免死金牌)
    @Query("SELECT c FROM Category c WHERE c.categoryId = 3 OR c.categoryType != 3 OR (c.categoryType = 3 AND EXISTS (" +
           "  SELECT 1 FROM DiscountCategory dc JOIN dc.discount d " +
           "  WHERE dc.category = c AND dc.categoryRole = 'Tag' " +
           "  AND d.status = 'active' " +
           "  AND CURRENT_DATE BETWEEN d.startDate AND d.endDate " +
           "  AND (" +
           "    EXISTS (SELECT 1 FROM DiscountProduct dp WHERE dp.discount = d) " +
           "    OR " +
           "    EXISTS (SELECT 1 FROM DiscountCategory dc2 WHERE dc2.discount = d AND (dc2.categoryRole = 'Main' OR dc2.categoryRole = 'Addon'))" +
           "  )" +
           "))")
    List<Category> findFrontEndCategories();
}

// findById, findAll, save, deleteById