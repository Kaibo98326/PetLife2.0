package com.petlife.repository;

import com.petlife.model.BeautyTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BeautyTimeSlotRepository extends JpaRepository<BeautyTimeSlot, Integer> {
    List<BeautyTimeSlot> findAllByOrderBySortOrderAsc();

    List<BeautyTimeSlot> findByIsBookableTrueOrderBySortOrderAsc();
}
