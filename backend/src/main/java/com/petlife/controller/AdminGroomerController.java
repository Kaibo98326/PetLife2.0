package com.petlife.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petlife.repository.GroomerManageRequest;
import com.petlife.repository.GroomerServiceRequest;
import com.petlife.service.GroomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/beauty/groomers")
public class AdminGroomerController {

    private final GroomerService groomerService;

    @GetMapping
    public ResponseEntity<?> getGroomers() {
        return ResponseEntity.ok(groomerService.getAllGroomers());
    }

    @PostMapping
    public ResponseEntity<?> createGroomer(@Valid @RequestBody GroomerManageRequest request) {
        return ResponseEntity.ok(groomerService.createGroomerWithServices(request));
    }

    @PutMapping
    public ResponseEntity<?> updateGroomer(@Valid @RequestBody GroomerManageRequest request) {
        return ResponseEntity.ok(groomerService.updateGroomerWithServices(request));
    }

    @GetMapping("/{groomerId}/services")
    public ResponseEntity<?> getServices(@PathVariable Integer groomerId) {
        return ResponseEntity.ok(groomerService.getServices(groomerId));
    }

    @PutMapping("/{groomerId}/services")
    public ResponseEntity<?> replaceServices(@PathVariable Integer groomerId,
            @Valid @RequestBody GroomerServiceRequest request) {

        return ResponseEntity.ok(groomerService.replaceServices(groomerId, request.beautyIds()));
    }
}
