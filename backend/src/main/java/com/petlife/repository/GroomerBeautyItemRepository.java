package com.petlife.repository;

import com.petlife.model.GroomerBeautyItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface GroomerBeautyItemRepository extends JpaRepository<GroomerBeautyItem, Integer> {
    List<GroomerBeautyItem> findByGroomerIdOrderByBeautyIdAsc(Integer groomerId);

    void deleteByGroomerId(Integer groomerId);

    @Query("""
                select count(distinct g.beautyId)
                from GroomerBeautyItem g
                where g.groomerId = :groomerId and g.isActive = true and g.beautyId in :beautyIds
            """)
    long countActiveServices(@Param("groomerId") Integer groomerId, @Param("beautyIds") List<Integer> beautyIds);
}
