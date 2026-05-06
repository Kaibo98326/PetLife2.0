package com.petlife.service;

import com.petlife.config.ApiException;
import com.petlife.repository.BeautyItemRequest;
import com.petlife.repository.BeautyPriceRequest;
import com.petlife.repository.BeautyItemResponse;
import com.petlife.model.BeautyItem;
import com.petlife.model.BeautyItemPrice;
import com.petlife.repository.BeautyItemPriceRepository;
import com.petlife.repository.BeautyItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BeautyItemService {

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

    public List<BeautyItem> getAllItems() {
        return itemRepository.findAllByOrderByBeautyIdAsc();
    }

    @Transactional
    public BeautyItem createItem(BeautyItemRequest req) {
        BeautyItem item = new BeautyItem();
        item.setItemName(req.itemName());
        item.setItemDescription(req.itemDescription());
        item.setDurationSlots(req.durationSlots());
        item.setIsActive(req.isActive() == null ? true : req.isActive());
        return itemRepository.save(item);
    }

    @Transactional
    public BeautyItem updateItem(Integer id, BeautyItemRequest req) {
        BeautyItem item = itemRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("找不到美容項目"));

        item.setItemName(req.itemName());
        item.setItemDescription(req.itemDescription());
        item.setDurationSlots(req.durationSlots());
        item.setIsActive(req.isActive() == null ? item.getIsActive() : req.isActive());
        return itemRepository.save(item);
    }

    public List<BeautyItemPrice> getPrices(Integer beautyId) {
        return priceRepository.findByBeautyIdOrderByPriceIdAsc(beautyId);
    }

    @Transactional
    public BeautyItemPrice createPrice(Integer beautyId, BeautyPriceRequest req) {
        itemRepository.findById(beautyId)
                .orElseThrow(() -> ApiException.notFound("找不到美容項目"));

        BeautyItemPrice price = new BeautyItemPrice();
        price.setBeautyId(beautyId);
        price.setPetSize(req.petSize());
        price.setItemPrice(req.itemPrice());
        price.setIsActive(req.isActive() == null ? true : req.isActive());
        return priceRepository.save(price);
    }

    @Transactional
    public BeautyItemPrice updatePrice(Integer priceId, BeautyPriceRequest req) {
        BeautyItemPrice price = priceRepository.findById(priceId)
                .orElseThrow(() -> ApiException.notFound("找不到價格"));

        price.setPetSize(req.petSize());
        price.setItemPrice(req.itemPrice());
        price.setIsActive(req.isActive() == null ? price.getIsActive() : req.isActive());
        return priceRepository.save(price);
    }
}
