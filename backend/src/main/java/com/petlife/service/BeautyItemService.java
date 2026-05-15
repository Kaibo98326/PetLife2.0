package com.petlife.service;

import com.petlife.config.ApiException;
import com.petlife.repository.BeautyItemManageRequest;
import com.petlife.repository.BeautyItemManageResponse;
import com.petlife.repository.BeautyItemRequest;
import com.petlife.repository.BeautyPriceLineRequest;
import com.petlife.repository.BeautyPriceRequest;
import com.petlife.repository.BeautyItemResponse;
import com.petlife.repository.BeautyPriceResponse;
import com.petlife.repository.UpdateBeautyItemStatusRequest;
import com.petlife.model.BeautyItem;
import com.petlife.model.BeautyItemPrice;
import com.petlife.repository.BeautyItemPriceRepository;
import com.petlife.repository.BeautyItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BeautyItemService {

    private static final Set<String> REQUIRED_PRICE_SIZES = Set.of("小型", "中型", "大型");
    private static final String DEFAULT_IMAGE_URL = "/images/beauty/default.jpg";
    private static final String BEAUTY_IMAGE_URL_PREFIX = "/images/beauty/";
    private static final Path BEAUTY_IMAGE_UPLOAD_DIR = Paths.get("C:/PetLife2.0/uploads/images/beauty/");

    private final BeautyItemRepository itemRepository;
    private final BeautyItemPriceRepository priceRepository;

    public BeautyItemService(BeautyItemRepository itemRepository, BeautyItemPriceRepository priceRepository) {
        this.itemRepository = itemRepository;
        this.priceRepository = priceRepository;
    }

    public List<BeautyItemResponse> getActiveItemsForPetSize(String petSize) {
        return itemRepository.findByIsActiveTrueOrderByBeautyIdAsc()
                .stream()
                .map(item -> {
                    BeautyItemPrice price = priceRepository
                            .findByBeautyIdAndPetSizeAndIsActiveTrue(item.getBeautyId(), petSize)
                            .orElse(null);
                    return BeautyMapper.item(item, price);
                })
                .filter(response -> response.itemPrice() != null)
                .toList();
    }

    public List<BeautyItemResponse> getAllItems() {
        return itemRepository.findAllByOrderByBeautyIdAsc()
                .stream()
                .map(BeautyMapper::item)
                .toList();
    }

    public List<BeautyItemManageResponse> getAllItemsWithPrices() {
        return itemRepository.findAllByOrderByBeautyIdAsc()
                .stream()
                .map(item -> BeautyMapper.itemManage(item,
                        priceRepository.findByBeautyIdOrderByPriceIdAsc(item.getBeautyId())))
                .toList();
    }

    public List<BeautyItemManageResponse> getActiveItemsWithPrices() {
        return itemRepository.findByIsActiveTrueOrderByBeautyIdAsc()
                .stream()
                .map(item -> BeautyMapper.itemManage(item,
                        priceRepository.findByBeautyIdOrderByPriceIdAsc(item.getBeautyId())
                                .stream()
                                .filter(price -> Boolean.TRUE.equals(price.getIsActive()))
                                .toList()))
                .toList();
    }

    public int calculateTotalSlots(List<Integer> beautyIds) {
        if (beautyIds == null || beautyIds.isEmpty()) {
            throw ApiException.badRequest("請至少選擇一個美容項目");
        }
        if (beautyIds.stream().anyMatch(Objects::isNull)) {
            throw ApiException.badRequest("美容項目代號不可為空");
        }

        List<Integer> distinctIds = beautyIds.stream().distinct().toList();
        List<BeautyItem> items = itemRepository.findAllById(distinctIds);

        if (items.size() != distinctIds.size()) {
            throw ApiException.badRequest("美容項目資料不存在");
        }

        int totalSlots = 0;
        for (BeautyItem item : items) {
            if (!Boolean.TRUE.equals(item.getIsActive())) {
                throw ApiException.badRequest("美容項目目前未啟用：" + item.getItemName());
            }
            if (item.getDurationSlots() == null || item.getDurationSlots() <= 0) {
                throw ApiException.badRequest("美容項目時長設定錯誤：" + item.getItemName());
            }
            totalSlots += item.getDurationSlots();
        }

        return totalSlots;
    }

    @Transactional
    public BeautyItemResponse createItem(BeautyItemRequest req) {
        BeautyItem item = new BeautyItem();
        item.setItemName(req.itemName());
        item.setItemDescription(req.itemDescription());
        item.setImageUrl(req.imageUrl());
        item.setDurationSlots(req.durationSlots());
        item.setIsActive(req.isActive() == null ? true : req.isActive());
        return BeautyMapper.item(itemRepository.save(item));
    }

    @Transactional
    public BeautyItemResponse updateItem(Integer id, BeautyItemRequest req) {
        BeautyItem item = itemRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("找不到美容項目"));

        item.setItemName(req.itemName());
        item.setItemDescription(req.itemDescription());
        item.setImageUrl(req.imageUrl());
        item.setDurationSlots(req.durationSlots());
        item.setIsActive(req.isActive() == null ? item.getIsActive() : req.isActive());
        return BeautyMapper.item(itemRepository.save(item));
    }

    @Transactional
    public BeautyItemManageResponse createItemWithPrices(BeautyItemManageRequest req) {
        validateManageRequest(req);

        BeautyItem item = new BeautyItem();
        applyItemFields(item, req);
        BeautyItem savedItem = itemRepository.save(item);

        List<BeautyItemPrice> prices = req.prices().stream()
                .map(priceReq -> createPriceEntity(savedItem.getBeautyId(), priceReq))
                .toList();

        return BeautyMapper.itemManage(savedItem, priceRepository.saveAll(prices));
    }

    @Transactional
    public BeautyItemManageResponse createItemWithPrices(BeautyItemManageRequest req, MultipartFile file) {
        validateManageRequest(req);

        BeautyItem item = new BeautyItem();
        applyItemFields(item, req);
        BeautyItem savedItem = itemRepository.save(item);

        if (hasImageFile(file)) {
            savedItem.setImageUrl(saveBeautyImage(savedItem.getBeautyId(), file));
            savedItem = itemRepository.save(savedItem);
        }

        Integer savedBeautyId = savedItem.getBeautyId();
        List<BeautyItemPrice> prices = req.prices().stream()
                .map(priceReq -> createPriceEntity(savedBeautyId, priceReq))
                .toList();

        return BeautyMapper.itemManage(savedItem, priceRepository.saveAll(prices));
    }

    @Transactional
    public BeautyItemManageResponse updateItemWithPrices(Integer beautyId, BeautyItemManageRequest req) {
        if (beautyId == null) {
            throw ApiException.badRequest("美容項目編號不可為空");
        }
        validateManageRequest(req);

        BeautyItem item = itemRepository.findById(beautyId)
                .orElseThrow(() -> ApiException.notFound("找不到美容項目"));

        applyItemFields(item, req);
        BeautyItem savedItem = itemRepository.save(item);

        List<BeautyItemPrice> prices = req.prices().stream()
                .map(priceReq -> priceRepository.findByBeautyIdAndPetSize(beautyId, priceReq.petSize())
                        .map(existing -> applyPriceFields(existing, priceReq))
                        .orElseGet(() -> createPriceEntity(beautyId, priceReq)))
                .toList();

        return BeautyMapper.itemManage(savedItem, priceRepository.saveAll(prices));
    }

    @Transactional
    public BeautyItemManageResponse updateItemWithPrices(Integer beautyId, BeautyItemManageRequest req,
            MultipartFile file) {
        if (beautyId == null) {
            throw ApiException.badRequest("美容項目編號不可為空");
        }
        validateManageRequest(req);

        BeautyItem item = itemRepository.findById(beautyId)
                .orElseThrow(() -> ApiException.notFound("找不到美容項目"));

        applyItemFields(item, req);
        if (hasImageFile(file)) {
            item.setImageUrl(saveBeautyImage(beautyId, file));
        }
        BeautyItem savedItem = itemRepository.save(item);

        List<BeautyItemPrice> prices = req.prices().stream()
                .map(priceReq -> priceRepository.findByBeautyIdAndPetSize(beautyId, priceReq.petSize())
                        .map(existing -> applyPriceFields(existing, priceReq))
                        .orElseGet(() -> createPriceEntity(beautyId, priceReq)))
                .toList();

        return BeautyMapper.itemManage(savedItem, priceRepository.saveAll(prices));
    }

    @Transactional
    public BeautyItemManageResponse updateItemStatus(Integer beautyId, UpdateBeautyItemStatusRequest req) {
        if (beautyId == null) {
            throw ApiException.badRequest("美容項目編號不可為空");
        }
        if (req == null || req.isActive() == null) {
            throw ApiException.badRequest("項目狀態不可為空");
        }

        BeautyItem item = itemRepository.findById(beautyId)
                .orElseThrow(() -> ApiException.notFound("找不到美容項目"));
        item.setIsActive(req.isActive());

        BeautyItem savedItem = itemRepository.save(item);
        return BeautyMapper.itemManage(savedItem, priceRepository.findByBeautyIdOrderByPriceIdAsc(beautyId));
    }

    public List<BeautyPriceResponse> getPrices(Integer beautyId) {
        return priceRepository.findByBeautyIdOrderByPriceIdAsc(beautyId)
                .stream()
                .map(BeautyMapper::price)
                .toList();
    }

    @Transactional
    public BeautyPriceResponse createPrice(Integer beautyId, BeautyPriceRequest req) {
        itemRepository.findById(beautyId)
                .orElseThrow(() -> ApiException.notFound("找不到美容項目"));

        BeautyItemPrice price = new BeautyItemPrice();
        price.setBeautyId(beautyId);
        price.setPetSize(req.petSize());
        price.setItemPrice(req.itemPrice());
        price.setIsActive(req.isActive() == null ? true : req.isActive());
        return BeautyMapper.price(priceRepository.save(price));
    }

    @Transactional
    public BeautyPriceResponse updatePrice(Integer priceId, BeautyPriceRequest req) {
        BeautyItemPrice price = priceRepository.findById(priceId)
                .orElseThrow(() -> ApiException.notFound("找不到價格"));

        price.setPetSize(req.petSize());
        price.setItemPrice(req.itemPrice());
        price.setIsActive(req.isActive() == null ? price.getIsActive() : req.isActive());
        return BeautyMapper.price(priceRepository.save(price));
    }

    private void validateManageRequest(BeautyItemManageRequest req) {
        if (req == null) {
            throw ApiException.badRequest("美容項目資料不可為空");
        }
        if (req.itemName() == null || req.itemName().isBlank()) {
            throw ApiException.badRequest("美容項目名稱不可為空");
        }
        if (req.durationSlots() == null || req.durationSlots() <= 0) {
            throw ApiException.badRequest("服務時段數必須大於 0");
        }
        if (req.prices() == null || req.prices().isEmpty()) {
            throw ApiException.badRequest("價格資料不可為空");
        }
        if (req.prices().stream().anyMatch(Objects::isNull)) {
            throw ApiException.badRequest("價格資料不可包含空值");
        }

        Set<String> petSizes = req.prices().stream()
                .map(BeautyPriceLineRequest::petSize)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!petSizes.equals(REQUIRED_PRICE_SIZES)) {
            throw ApiException.badRequest("價格必須包含小型、中型、大型");
        }

        Map<String, Long> petSizeCounts = req.prices().stream()
                .collect(Collectors.groupingBy(BeautyPriceLineRequest::petSize, Collectors.counting()));
        if (petSizeCounts.values().stream().anyMatch(count -> count > 1)) {
            throw ApiException.badRequest("同一體型不可重複設定價格");
        }

        req.prices().forEach(price -> {
            if (price.petSize() == null || price.petSize().isBlank()) {
                throw ApiException.badRequest("寵物體型不可為空");
            }
            if (price.itemPrice() == null || price.itemPrice().signum() < 0) {
                throw ApiException.badRequest("價格不可為空或小於 0");
            }
        });
    }

    private void applyItemFields(BeautyItem item, BeautyItemManageRequest req) {
        item.setItemName(req.itemName());
        item.setItemDescription(req.itemDescription());
        item.setImageUrl(normalizeImageUrl(req.imageUrl()));
        item.setDurationSlots(req.durationSlots());
        item.setIsActive(req.isActive() == null ? true : req.isActive());
    }

    private boolean hasImageFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private String normalizeImageUrl(String imageUrl) {
        return imageUrl == null || imageUrl.isBlank() ? DEFAULT_IMAGE_URL : imageUrl.trim();
    }

    private String saveBeautyImage(Integer beautyId, MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw ApiException.badRequest("請上傳圖片檔案");
        }

        try {
            Files.createDirectories(BEAUTY_IMAGE_UPLOAD_DIR);

            String originalName = file.getOriginalFilename();
            String safeName = originalName == null ? "beauty.jpg" : originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            if (safeName.isBlank()) {
                safeName = "beauty.jpg";
            }

            String fileName = "beauty_" + beautyId + "_" + System.currentTimeMillis() + "_" + safeName;
            Path targetPath = BEAUTY_IMAGE_UPLOAD_DIR.resolve(fileName).normalize();
            if (!targetPath.startsWith(BEAUTY_IMAGE_UPLOAD_DIR)) {
                throw ApiException.badRequest("圖片檔名不合法");
            }

            Files.copy(file.getInputStream(), targetPath);
            return BEAUTY_IMAGE_URL_PREFIX + fileName;
        } catch (IOException e) {
            throw ApiException.badRequest("圖片上傳失敗");
        }
    }

    private BeautyItemPrice createPriceEntity(Integer beautyId, BeautyPriceLineRequest req) {
        BeautyItemPrice price = new BeautyItemPrice();
        price.setBeautyId(beautyId);
        return applyPriceFields(price, req);
    }

    private BeautyItemPrice applyPriceFields(BeautyItemPrice price, BeautyPriceLineRequest req) {
        price.setPetSize(req.petSize());
        price.setItemPrice(req.itemPrice());
        price.setIsActive(req.isActive() == null ? true : req.isActive());
        return price;
    }
}
