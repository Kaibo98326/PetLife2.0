package com.petlife.repository;

import com.petlife.model.GroomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroomerProfileRepository extends JpaRepository<GroomerProfile, Integer> {
    List<GroomerProfile> findByIsBookableTrueOrderByGroomerIdAsc();

    List<GroomerProfile> findAllByOrderByGroomerIdAsc();
}
