package com.petlife.repository;

import com.petlife.model.BeautyAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface BeautyAppointmentRepository extends JpaRepository<BeautyAppointment, Integer> {
    List<BeautyAppointment> findByMemberIdOrderByAppointDateDescAppointmentIdDesc(Integer memberId);

    List<BeautyAppointment> findAllByOrderByAppointDateDescAppointmentIdDesc();

    List<BeautyAppointment> findByAppointDateBetweenOrderByAppointDateDescAppointmentIdDesc(LocalDate startDate,
            LocalDate endDate);
}
