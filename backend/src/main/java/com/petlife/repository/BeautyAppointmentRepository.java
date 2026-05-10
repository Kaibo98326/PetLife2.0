package com.petlife.repository;

import com.petlife.model.BeautyAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface BeautyAppointmentRepository extends JpaRepository<BeautyAppointment, Integer> {
    List<BeautyAppointment> findByMemberIdOrderByAppointDateDescAppointmentIdDesc(Integer memberId);

    List<BeautyAppointment> findAllByOrderByAppointDateDescAppointmentIdDesc();

    List<BeautyAppointment> findByAppointDateBetweenOrderByAppointDateDescAppointmentIdDesc(LocalDate startDate,
            LocalDate endDate);

    @Query("""
                select a
                from BeautyAppointment a
                where (:startDate is null or a.appointDate >= :startDate)
                  and (:endDate is null or a.appointDate <= :endDate)
                  and (:status is null or a.appointmentStatus = :status)
                  and (:groomerId is null or a.groomerId = :groomerId)
                order by a.appointDate desc, a.appointmentId desc
            """)
    List<BeautyAppointment> searchAdminAppointments(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") String status,
            @Param("groomerId") Integer groomerId);
}
