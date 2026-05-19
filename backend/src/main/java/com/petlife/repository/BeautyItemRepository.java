package com.petlife.repository;

import com.petlife.model.BeautyItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BeautyItemRepository extends JpaRepository<BeautyItem, Integer> {
    List<BeautyItem> findByIsActiveTrueOrderByBeautyIdAsc();

    List<BeautyItem> findAllByOrderByBeautyIdAsc();
}
