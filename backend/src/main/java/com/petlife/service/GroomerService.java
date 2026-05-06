package com.petlife.service;

import com.petlife.config.ApiException;
import com.petlife.repository.GroomerProfileRequest;
import com.petlife.repository.GroomerResponse;
import com.petlife.model.GroomerBeautyItem;
import com.petlife.model.GroomerProfile;
import com.petlife.repository.GroomerBeautyItemRepository;
import com.petlife.repository.GroomerProfileRepository;
import com.petlife.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroomerService {

    private final GroomerProfileRepository groomerRepository;
    private final GroomerBeautyItemRepository serviceRepository;
    private final EmployeeRepository employeeRepository;

    public GroomerService(
            GroomerProfileRepository groomerRepository,
            GroomerBeautyItemRepository serviceRepository,
            EmployeeRepository employeeRepository) {
        this.groomerRepository = groomerRepository;
        this.serviceRepository = serviceRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<GroomerResponse> getAllGroomers() {
        return groomerRepository.findAllByOrderByGroomerIdAsc()
                .stream()
                .map(BeautyMapper::groomer)
                .toList();
    }

    @Transactional
    public GroomerProfile upsertGroomer(GroomerProfileRequest req) {
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

    public List<GroomerBeautyItem> getServices(Integer groomerId) {
        return serviceRepository.findByGroomerIdOrderByBeautyIdAsc(groomerId);
    }

    @Transactional
    public List<GroomerBeautyItem> replaceServices(Integer groomerId, List<Integer> beautyIds) {
        groomerRepository.findById(groomerId)
                .orElseThrow(() -> ApiException.notFound("找不到美容師"));

        serviceRepository.deleteByGroomerId(groomerId);

        return serviceRepository.saveAll(beautyIds.stream().distinct().map(beautyId -> {
            GroomerBeautyItem row = new GroomerBeautyItem();
            row.setGroomerId(groomerId);
            row.setBeautyId(beautyId);
            row.setIsActive(true);
            return row;
        }).toList());
    }
}
