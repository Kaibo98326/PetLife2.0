package com.petlife.service;

import com.petlife.config.ApiException;
import com.petlife.repository.GroomerManageRequest;
import com.petlife.repository.GroomerManageResponse;
import com.petlife.repository.GroomerProfileRequest;
import com.petlife.repository.GroomerResponse;
import com.petlife.repository.GroomerServiceResponse;
import com.petlife.model.BeautyItem;
import com.petlife.model.GroomerBeautyItem;
import com.petlife.model.GroomerProfile;
import com.petlife.repository.BeautyItemRepository;
import com.petlife.repository.GroomerBeautyItemRepository;
import com.petlife.repository.GroomerProfileRepository;
import com.petlife.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GroomerService {

    private final GroomerProfileRepository groomerRepository;
    private final GroomerBeautyItemRepository serviceRepository;
    private final EmployeeRepository employeeRepository;
    private final BeautyItemRepository itemRepository;

    public GroomerService(
            GroomerProfileRepository groomerRepository,
            GroomerBeautyItemRepository serviceRepository,
            EmployeeRepository employeeRepository,
            BeautyItemRepository itemRepository) {
        this.groomerRepository = groomerRepository;
        this.serviceRepository = serviceRepository;
        this.employeeRepository = employeeRepository;
        this.itemRepository = itemRepository;
    }

    public List<GroomerResponse> getAllGroomers() {
        return groomerRepository.findAllByOrderByGroomerIdAsc()
                .stream()
                .map(BeautyMapper::groomer)
                .toList();
    }

    @Transactional
    public GroomerResponse upsertGroomer(GroomerProfileRequest req) {
        return BeautyMapper.groomer(saveGroomerProfile(req));
    }

    @Transactional
    public GroomerManageResponse upsertGroomerWithServices(GroomerManageRequest req) {
        validateManageRequest(req);

        GroomerProfile groomer = saveGroomerProfile(new GroomerProfileRequest(
                req.groomerId(),
                req.displayName(),
                req.intro(),
                req.seniorityYears(),
                req.isBookable()));
        List<GroomerServiceResponse> services = replaceServices(groomer.getGroomerId(), req.beautyIds());

        return BeautyMapper.groomerManage(groomer, services);
    }

    @Transactional
    public GroomerManageResponse createGroomerWithServices(GroomerManageRequest req) {
        validateManageRequest(req);

        if (groomerRepository.existsById(req.groomerId())) {
            throw ApiException.badRequest("該員工已是美容師");
        }

        GroomerProfile groomer = saveGroomerProfile(new GroomerProfileRequest(
                req.groomerId(),
                req.displayName(),
                req.intro(),
                req.seniorityYears(),
                req.isBookable()));
        List<GroomerServiceResponse> services = replaceServices(groomer.getGroomerId(), req.beautyIds());

        return BeautyMapper.groomerManage(groomer, services);
    }

    @Transactional
    public GroomerManageResponse updateGroomerWithServices(GroomerManageRequest req) {
        validateManageRequest(req);

        if (!groomerRepository.existsById(req.groomerId())) {
            throw ApiException.notFound("找不到美容師");
        }

        GroomerProfile groomer = saveGroomerProfile(new GroomerProfileRequest(
                req.groomerId(),
                req.displayName(),
                req.intro(),
                req.seniorityYears(),
                req.isBookable()));
        List<GroomerServiceResponse> services = replaceServices(groomer.getGroomerId(), req.beautyIds());

        return BeautyMapper.groomerManage(groomer, services);
    }

    public List<GroomerServiceResponse> getServices(Integer groomerId) {
        return serviceRepository.findByGroomerIdOrderByBeautyIdAsc(groomerId)
                .stream()
                .filter(service -> Boolean.TRUE.equals(service.getIsActive()))
                .map(BeautyMapper::groomerService)
                .toList();
    }

    @Transactional
    public List<GroomerServiceResponse> replaceServices(Integer groomerId, List<Integer> beautyIds) {
        validateReplaceServicesRequest(groomerId, beautyIds);

        groomerRepository.findById(groomerId)
                .orElseThrow(() -> ApiException.notFound("找不到美容師"));

        List<Integer> distinctBeautyIds = beautyIds.stream().distinct().toList();
        List<BeautyItem> items = itemRepository.findAllById(distinctBeautyIds);

        if (items.size() != distinctBeautyIds.size()) {
            throw ApiException.badRequest("部分美容項目不存在");
        }

        items.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getIsActive()))
                .findFirst()
                .ifPresent(item -> {
                    throw ApiException.badRequest("美容項目已停用：" + item.getItemName());
                });

        List<GroomerBeautyItem> existingServices = serviceRepository.findByGroomerIdOrderByBeautyIdAsc(groomerId);
        Map<Integer, GroomerBeautyItem> existingByBeautyId = existingServices.stream()
                .collect(Collectors.toMap(GroomerBeautyItem::getBeautyId, Function.identity()));
        Set<Integer> requestedBeautyIds = Set.copyOf(distinctBeautyIds);
        List<GroomerBeautyItem> rowsToSave = new ArrayList<>();

        for (Integer beautyId : distinctBeautyIds) {
            GroomerBeautyItem row = existingByBeautyId.get(beautyId);
            if (row == null) {
                row = new GroomerBeautyItem();
                row.setGroomerId(groomerId);
                row.setBeautyId(beautyId);
            }
            row.setIsActive(true);
            rowsToSave.add(row);
        }

        existingServices.stream()
                .filter(row -> !requestedBeautyIds.contains(row.getBeautyId()))
                .filter(row -> Boolean.TRUE.equals(row.getIsActive()))
                .forEach(row -> {
                    row.setIsActive(false);
                    rowsToSave.add(row);
                });

        return serviceRepository.saveAll(rowsToSave)
                .stream()
                .filter(service -> Boolean.TRUE.equals(service.getIsActive()))
                .map(BeautyMapper::groomerService)
                .toList();
    }

    private void validateReplaceServicesRequest(Integer groomerId, List<Integer> beautyIds) {
        if (groomerId == null) {
            throw ApiException.badRequest("美容師編號不可為空");
        }
        if (beautyIds == null || beautyIds.isEmpty()) {
            throw ApiException.badRequest("至少需選擇一個美容項目");
        }
        if (beautyIds.stream().anyMatch(Objects::isNull)) {
            throw ApiException.badRequest("美容項目編號不可為空");
        }
    }

    private GroomerProfile saveGroomerProfile(GroomerProfileRequest req) {
        if (req == null || req.groomerId() == null) {
            throw ApiException.badRequest("美容師資料不可為空");
        }

        employeeRepository.findById(req.groomerId())
                .orElseThrow(() -> ApiException.badRequest("找不到對應員工"));

        GroomerProfile groomer = groomerRepository.findById(req.groomerId())
                .orElseGet(GroomerProfile::new);

        groomer.setGroomerId(req.groomerId());
        groomer.setDisplayName(req.displayName());
        groomer.setIntro(req.intro());
        groomer.setSeniorityYears(req.seniorityYears());
        groomer.setIsBookable(req.isBookable() == null ? true : req.isBookable());
        return groomerRepository.save(groomer);
    }

    private void validateManageRequest(GroomerManageRequest req) {
        if (req == null) {
            throw ApiException.badRequest("美容師資料不可為空");
        }
        validateReplaceServicesRequest(req.groomerId(), req.beautyIds());
    }
}
