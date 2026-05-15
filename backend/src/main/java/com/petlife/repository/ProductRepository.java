package com.petlife.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.petlife.model.Product;


@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {

	
//===== 分類查詢 ================================================================================================

	@Query("SELECT p FROM Product p JOIN p.categories c WHERE c.categoryId = :catId OR c.parentId = :catId")
	Page<Product> findByCategory(@Param("catId") Integer categoryId, Pageable pageable);

	@Query("SELECT p FROM Product p JOIN p.categories c WHERE (c.categoryId = :catId OR c.parentId = :catId) AND p.productStatus = 1")
	Page<Product> findActiveByCategory(@Param("catId") Integer categoryId, Pageable pageable);

    
//===== 關鍵字查詢 ==============================================================================================

	@Query("SELECT p FROM Product p WHERE p.productName LIKE %:kw%")
    Page<Product> searchByName(@Param("kw") String keyword, Pageable pageable);
    
	@Query("SELECT p FROM Product p WHERE p.productName LIKE %:kw% AND p.productStatus = 1")
    Page<Product> searchActiveByName(@Param("kw") String keyword, Pageable pageable);
    
    
	
//===== 統計筆數 ===============================================================================================
    
   	@Query("SELECT COUNT(p) FROM Product p JOIN p.categories c WHERE c.categoryId = :catId")
	long countByCategoryId(@Param("catId") Integer categoryId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.productName LIKE %:kw%")
    long countByKeyword(@Param("kw") String keyword);
    

//===== 庫存預警查詢 ======================================================================================
    
    @Query("SELECT p FROM Product p WHERE p.productStock <= COALESCE(p.lowStock, 10) ORDER BY p.productId DESC")
    Page<Product> findLowStock(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.productStock <= COALESCE(p.lowStock, 10)")
    long countLowStock();
    
//===== 後台商品 批次上下架處理 ======================================================================================
    
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.productStatus = :status WHERE p.productId IN :ids")
    void batchUpdateStatus(@Param("ids") List<Integer> ids, @Param("status") Integer status);

//===== 熱門排行 (依點擊率) ======================================================================================
    
    @Query("SELECT p FROM Product p WHERE p.productStatus = 1 ORDER BY p.clickCount DESC, p.productId DESC")
    List<Product> findTop10ByClickCount(Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.clickCount = COALESCE(p.clickCount, 0) + 1 WHERE p.productId = :id")
    void incrementClickCount(@Param("id") Integer productId);
    
 // ✨ 新增：活動標籤查詢專用 ======================================================================================

    // 透過多個商品 ID 取得分頁商品
    @Query("SELECT p FROM Product p WHERE p.productId IN :ids")
    Page<Product> findByProductIdIn(@Param("ids") List<Integer> ids, Pageable pageable);

    // 透過多個分類 ID 撈取底下的所有商品 ID
    @Query("SELECT p.productId FROM Product p JOIN p.categories c WHERE c.categoryId IN :catIds OR c.parentId IN :catIds")
    List<Integer> findProductIdsByCategoryIds(@Param("catIds") List<Integer> catIds);
    
}


