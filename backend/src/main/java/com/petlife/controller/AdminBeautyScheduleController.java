package com.petlife.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petlife.config.ApiException;
import com.petlife.repository.GroomerScheduleRequest;
import com.petlife.repository.UpdateDayScheduleSlotsRequest;
import com.petlife.service.GroomerScheduleService;
import com.petlife.service.GroomerWorkSlotService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/beauty/schedules")
public class AdminBeautyScheduleController {

    private final GroomerScheduleService groomerScheduleService;
    private final GroomerWorkSlotService groomerWorkSlotService;

    @GetMapping
    public ResponseEntity<?> getSchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(groomerScheduleService.findSchedules(startDate, endDate));
    }

    @GetMapping("/month")
    public ResponseEntity<?> getMonthlySchedule(@RequestParam Integer groomerId,
            @RequestParam String yearMonth) {

        return ResponseEntity.ok(groomerScheduleService.getMonthlySchedule(groomerId, parseYearMonth(yearMonth)));
    }

    @PutMapping
    public ResponseEntity<?> upsertSchedule(@Valid @RequestBody GroomerScheduleRequest request) {
        return ResponseEntity.ok(groomerScheduleService.upsertSchedule(request));
    }

    @GetMapping("/day-slots")
    public ResponseEntity<?> getDaySlotStatus(@RequestParam Integer groomerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate) {

        return ResponseEntity.ok(groomerWorkSlotService.getDaySlotStatus(groomerId, workDate));
    }

    @PutMapping("/day-slots")
    public ResponseEntity<?> updateDayScheduleSlots(@Valid @RequestBody UpdateDayScheduleSlotsRequest request) {
        return ResponseEntity.ok(groomerScheduleService.updateDayScheduleSlots(request));
    }

    private YearMonth parseYearMonth(String yearMonth) {
        try {
            return YearMonth.parse(yearMonth);
        } catch (DateTimeParseException ex) {
            throw ApiException.badRequest("年月格式必須是 yyyy-MM");
        }
    }
}
