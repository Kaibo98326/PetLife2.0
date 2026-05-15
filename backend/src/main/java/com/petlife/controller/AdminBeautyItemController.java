package com.petlife.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.petlife.repository.BeautyItemManageRequest;
import com.petlife.repository.BeautyPriceRequest;
import com.petlife.repository.UpdateBeautyItemStatusRequest;
import com.petlife.service.BeautyItemService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/beauty/items")
public class AdminBeautyItemController {

    private final BeautyItemService beautyItemService;

    @GetMapping
    public ResponseEntity<?> getItems() {
        return ResponseEntity.ok(beautyItemService.getAllItemsWithPrices());
    }

    @PostMapping
    public ResponseEntity<?> createItem(@Valid @RequestBody BeautyItemManageRequest request) {
        return ResponseEntity.ok(beautyItemService.createItemWithPrices(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createItemWithImage(@Valid @RequestPart("request") BeautyItemManageRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        return ResponseEntity.ok(beautyItemService.createItemWithPrices(request, file));
    }

    @PutMapping("/{beautyId}")
    public ResponseEntity<?> updateItem(@PathVariable Integer beautyId,
            @Valid @RequestBody BeautyItemManageRequest request) {

        return ResponseEntity.ok(beautyItemService.updateItemWithPrices(beautyId, request));
    }

    @PutMapping(value = "/{beautyId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateItemWithImage(@PathVariable Integer beautyId,
            @Valid @RequestPart("request") BeautyItemManageRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        return ResponseEntity.ok(beautyItemService.updateItemWithPrices(beautyId, request, file));
    }

    @PutMapping("/{beautyId}/status")
    public ResponseEntity<?> updateItemStatus(@PathVariable Integer beautyId,
            @Valid @RequestBody UpdateBeautyItemStatusRequest request) {

        return ResponseEntity.ok(beautyItemService.updateItemStatus(beautyId, request));
    }

    @GetMapping("/{beautyId}/prices")
    public ResponseEntity<?> getPrices(@PathVariable Integer beautyId) {
        return ResponseEntity.ok(beautyItemService.getPrices(beautyId));
    }

    @PostMapping("/{beautyId}/prices")
    public ResponseEntity<?> createPrice(@PathVariable Integer beautyId,
            @Valid @RequestBody BeautyPriceRequest request) {

        return ResponseEntity.ok(beautyItemService.createPrice(beautyId, request));
    }

    @PutMapping("/prices/{priceId}")
    public ResponseEntity<?> updatePrice(@PathVariable Integer priceId,
            @Valid @RequestBody BeautyPriceRequest request) {

        return ResponseEntity.ok(beautyItemService.updatePrice(priceId, request));
    }
}
