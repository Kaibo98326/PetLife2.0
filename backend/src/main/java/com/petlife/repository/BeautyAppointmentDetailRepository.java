package com.petlife.repository;

import com.petlife.model.BeautyAppointmentDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BeautyAppointmentDetailRepository extends JpaRepository<BeautyAppointmentDetail, Integer> {
    List<BeautyAppointmentDetail> findByAppointmentIdOrderByLineNoAsc(Integer appointmentId);
}
