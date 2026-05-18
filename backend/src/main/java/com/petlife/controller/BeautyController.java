package com.petlife.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petlife.config.ApiException;
import com.petlife.repository.CancelAppointmentRequest;
import com.petlife.repository.CreateBeautyAppointmentRequest;
import com.petlife.service.BeautyAppointmentService;
import com.petlife.service.BeautyAvailabilityService;
import com.petlife.service.BeautyItemService;
import com.petlife.service.JwtUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/beauty")
public class BeautyController {

    private final BeautyItemService beautyItemService;
    private final BeautyAvailabilityService beautyAvailabilityService;
    private final BeautyAppointmentService beautyAppointmentService;
    private final JwtUtils jwtUtils;

    @GetMapping("/items")
    public ResponseEntity<?> getItems() {
        return ResponseEntity.ok(beautyItemService.getActiveItemsWithPrices());
    }

    @GetMapping("/groomers")
    public ResponseEntity<?> getAvailableGroomers(
            @RequestParam List<Integer> beautyIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(beautyAvailabilityService.findAvailableGroomers(beautyIds, date));
    }

    @GetMapping("/available-slots")
    public ResponseEntity<?> getAvailableSlots(
            @RequestParam Integer groomerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam List<Integer> beautyIds) {

        int totalSlots = beautyItemService.calculateTotalSlots(beautyIds);
        return ResponseEntity.ok(beautyAvailabilityService.findAvailableStartSlots(groomerId, date, totalSlots));
    }

    @PostMapping("/appointments")
    public ResponseEntity<?> createAppointment(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateBeautyAppointmentRequest request) {

        return ResponseEntity.ok(beautyAppointmentService.createAppointment(resolveMemberId(authorization), request));
    }

    @GetMapping("/appointments/my")
    public ResponseEntity<?> getMyAppointments(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.ok(beautyAppointmentService.getMemberAppointments(resolveMemberId(authorization)));
    }

    @GetMapping("/appointments/{appointmentId}")
    public ResponseEntity<?> getAppointment(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Integer appointmentId) {

        return ResponseEntity.ok(
                beautyAppointmentService.getMemberAppointment(resolveMemberId(authorization), appointmentId));
    }

    @PostMapping("/appointments/{appointmentId}/cancel")
    public ResponseEntity<?> cancelAppointment(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Integer appointmentId,
            @RequestBody(required = false) CancelAppointmentRequest request) {

        return ResponseEntity.ok(
                beautyAppointmentService.cancelByMember(resolveMemberId(authorization), appointmentId, request));
    }

    private Integer resolveMemberId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw ApiException.forbidden("請先登入會員");
        }

        String memberId = jwtUtils.validateToken(authorization.replace("Bearer ", ""));
        if (memberId == null) {
            throw ApiException.forbidden("會員登入狀態已失效，請重新登入");
        }

        return Integer.valueOf(memberId);
    }
}
