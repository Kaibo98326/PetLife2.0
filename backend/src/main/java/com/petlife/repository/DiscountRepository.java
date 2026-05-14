package com.petlife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.petlife.model.Discount;
import java.util.List;

public interface DiscountRepository extends JpaRepository<Discount, Integer> {
    
    // ✨ 新增：透過活動標籤 ID 尋找「進行中」的活動
    @Query("SELECT d FROM Discount d JOIN d.discountCategories dc " +
           "WHERE dc.category.categoryId = :tagId AND dc.categoryRole = 'Tag' " +
           "AND d.status = 'active' " +
           "AND d.startDate <= :today AND d.endDate >= :today")
    List<Discount> findActiveDiscountsByTagId(@Param("tagId") Integer tagId, @Param("today") java.time.LocalDate today);
}