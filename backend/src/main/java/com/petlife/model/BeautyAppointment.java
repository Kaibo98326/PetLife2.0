package com.petlife.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "BeautyAppointment")
public class BeautyAppointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appointment_id")
    private Integer appointmentId;

    @Column(name = "member_id", nullable = false)
    private Integer memberId;

    @Column(name = "pet_id", nullable = false)
    private Integer petId;

    @Column(name = "groomer_id", nullable = false)
    private Integer groomerId;

    @Column(name = "appoint_date", nullable = false)
    private LocalDate appointDate;

    @Column(name = "start_slot_id", nullable = false)
    private Integer startSlotId;

    @Column(name = "pet_size_snapshot", nullable = false, length = 10)
    private String petSizeSnapshot;

    @Column(name = "total_slots", nullable = false)
    private Integer totalSlots;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "appointment_status", nullable = false, length = 20)
    private String appointmentStatus = "待確認";

    @Column(name = "contact_note", length = 500)
    private String contactNote;

    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (appointmentStatus == null) {
            appointmentStatus = "待確認";
        }
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
