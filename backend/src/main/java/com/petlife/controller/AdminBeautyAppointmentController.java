package com.petlife.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petlife.repository.UpdateAppointmentStatusRequest;
import com.petlife.service.AdminBeautyAppointmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/beauty/appointments")
public class AdminBeautyAppointmentController {

    private final AdminBeautyAppointmentService adminBeautyAppointmentService;

    @GetMapping
    public ResponseEntity<?> getAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer groomerId) {

        return ResponseEntity.ok(adminBeautyAppointmentService.findAppointments(startDate, endDate, status, groomerId));
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<?> getAppointment(@PathVariable Integer appointmentId) {
        return ResponseEntity.ok(adminBeautyAppointmentService.getAppointment(appointmentId));
    }

    @PutMapping("/{appointmentId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Integer appointmentId,
            @Valid @RequestBody UpdateAppointmentStatusRequest request) {

        return ResponseEntity.ok(adminBeautyAppointmentService.updateStatus(appointmentId, request));
    }
}
