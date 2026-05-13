package com.petlife.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.petlife.config.BeautyConstants;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "GroomerWorkSlot")
public class GroomerWorkSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_slot_id")
    private Integer workSlotId;

    @Column(name = "groomer_id", nullable = false)
    private Integer groomerId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "slot_id", nullable = false)
    private Integer slotId;

    @Column(name = "appointment_id")
    private Integer appointmentId;

    @Column(name = "work_slot_status", nullable = false, length = 20)
    private String workSlotStatus = BeautyConstants.WORK_SLOT_APPOINTMENT;

    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (workSlotStatus == null) {
            workSlotStatus = BeautyConstants.WORK_SLOT_APPOINTMENT;
        }
    }
}
