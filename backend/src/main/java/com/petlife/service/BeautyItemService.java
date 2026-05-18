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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    public List<String> getBeautyImageUrls() {
        try {
            Files.createDirectories(BEAUTY_IMAGE_UPLOAD_DIR);
            try (var stream = Files.list(BEAUTY_IMAGE_UPLOAD_DIR)) {
                return stream
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .filter(this::isSupportedBeautyImageName)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .map(fileName -> BEAUTY_IMAGE_URL_PREFIX + fileName)
                        .toList();
            }
        } catch (IOException e) {
            throw ApiException.badRequest("圖片清單讀取失敗");
        }
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

        String oldImageUrl = item.getImageUrl();
        applyItemFields(item, req);
        if (hasImageFile(file)) {
            String newImageUrl = saveBeautyImage(beautyId, file);
            item.setImageUrl(newImageUrl);
            deleteStaleBeautyImages(beautyId, oldImageUrl, newImageUrl);
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

    private boolean isSupportedBeautyImageName(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".png")
                || lowerName.endsWith(".gif")
                || lowerName.endsWith(".webp")
                || lowerName.endsWith(".bmp");
    }

    private void deleteStaleBeautyImages(Integer beautyId, String oldImageUrl, String currentImageUrl) {
        deleteBeautyImage(oldImageUrl, currentImageUrl);

        String currentFileName = getBeautyImageFileName(currentImageUrl);
        String oldTimestampPrefix = "beauty_" + beautyId + "_";
        String fixedNamePrefix = "beauty_" + beautyId + ".";

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BEAUTY_IMAGE_UPLOAD_DIR)) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                if (fileName.equals(currentFileName)) {
                    continue;
                }
                if (fileName.startsWith(oldTimestampPrefix) || fileName.startsWith(fixedNamePrefix)) {
                    Files.deleteIfExists(path);
                }
            }
        } catch (IOException e) {
            throw ApiException.badRequest("舊圖片刪除失敗");
        }
    }

    private void deleteBeautyImage(String imageUrl, String currentImageUrl) {
        if (imageUrl == null || imageUrl.isBlank() || DEFAULT_IMAGE_URL.equals(imageUrl)) {
            return;
        }
        if (!imageUrl.startsWith(BEAUTY_IMAGE_URL_PREFIX)) {
            return;
        }

        String fileName = getBeautyImageFileName(imageUrl);
        if (fileName.isBlank()) {
            return;
        }
        if (fileName.equals(getBeautyImageFileName(currentImageUrl))) {
            return;
        }

        try {
            Path targetPath = BEAUTY_IMAGE_UPLOAD_DIR.resolve(fileName).normalize();
            if (!targetPath.startsWith(BEAUTY_IMAGE_UPLOAD_DIR)) {
                return;
            }
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            throw ApiException.badRequest("舊圖片刪除失敗");
        }
    }

    private String getBeautyImageFileName(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(BEAUTY_IMAGE_URL_PREFIX)) {
            return "";
        }
        return imageUrl.substring(BEAUTY_IMAGE_URL_PREFIX.length());
    }

    private String saveBeautyImage(Integer beautyId, MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw ApiException.badRequest("請上傳圖片檔案");
        }

        try {
            Files.createDirectories(BEAUTY_IMAGE_UPLOAD_DIR);

            String fileName = "beauty_" + beautyId + resolveImageExtension(contentType, file.getOriginalFilename());
            Path targetPath = BEAUTY_IMAGE_UPLOAD_DIR.resolve(fileName).normalize();
            if (!targetPath.startsWith(BEAUTY_IMAGE_UPLOAD_DIR)) {
                throw ApiException.badRequest("圖片檔名不合法");
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return BEAUTY_IMAGE_URL_PREFIX + fileName;
        } catch (IOException e) {
            throw ApiException.badRequest("圖片上傳失敗");
        }
    }

    private String resolveImageExtension(String contentType, String originalName) {
        String normalizedContentType = contentType.toLowerCase();
        if (normalizedContentType.contains("jpeg") || normalizedContentType.contains("jpg")) {
            return ".jpg";
        }
        if (normalizedContentType.contains("png")) {
            return ".png";
        }
        if (normalizedContentType.contains("gif")) {
            return ".gif";
        }
        if (normalizedContentType.contains("webp")) {
            return ".webp";
        }
        if (normalizedContentType.contains("bmp")) {
            return ".bmp";
        }

        if (originalName != null) {
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < originalName.length() - 1) {
                String extension = originalName.substring(dotIndex).toLowerCase();
                if (extension.matches("\\.[a-z0-9]{1,5}")) {
                    return extension;
                }
            }
        }

        return ".jpg";
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
