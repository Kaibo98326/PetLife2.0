package com.petlife.repository;

import com.petlife.model.GroomerWorkSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface GroomerWorkSlotRepository extends JpaRepository<GroomerWorkSlot, Integer> {
    List<GroomerWorkSlot> findByGroomerIdAndWorkDate(Integer groomerId, LocalDate workDate);

    List<GroomerWorkSlot> findByGroomerIdAndWorkDateAndSlotIdIn(Integer groomerId, LocalDate workDate,
            Collection<Integer> slotIds);

    boolean existsByGroomerIdAndWorkDateAndSlotIdIn(Integer groomerId, LocalDate workDate, Collection<Integer> slotIds);

    void deleteByAppointmentIdAndWorkSlotStatus(Integer appointmentId, String workSlotStatus);
}
