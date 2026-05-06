package com.petlife.repository;

import com.petlife.model.BeautyItemPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BeautyItemPriceRepository extends JpaRepository<BeautyItemPrice, Integer> {
    Optional<BeautyItemPrice> findByBeautyIdAndPetSizeAndIsActiveTrue(Integer beautyId, String petSize);

    List<BeautyItemPrice> findByBeautyIdOrderByPriceIdAsc(Integer beautyId);
}
