package com.petlife.repository;

import com.petlife.model.GroomerSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GroomerScheduleRepository extends JpaRepository<GroomerSchedule, Integer> {
    Optional<GroomerSchedule> findByGroomerIdAndWorkDate(Integer groomerId, LocalDate workDate);

    List<GroomerSchedule> findByWorkDateBetweenOrderByWorkDateAscGroomerIdAsc(LocalDate startDate, LocalDate endDate);
}
