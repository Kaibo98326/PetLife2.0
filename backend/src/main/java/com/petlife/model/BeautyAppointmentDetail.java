package com.petlife.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "BeautyAppointmentDetail")
public class BeautyAppointmentDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Integer detailId;

    @Column(name = "appointment_id", nullable = false)
    private Integer appointmentId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "beauty_id", nullable = false)
    private Integer beautyId;

    @Column(name = "item_name_snapshot", nullable = false, length = 50)
    private String itemNameSnapshot;

    @Column(name = "item_price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal itemPriceSnapshot;

    @Column(name = "duration_slots_snapshot", nullable = false)
    private Integer durationSlotsSnapshot;

    @Column(name = "line_note", length = 200)
    private String lineNote;
}
