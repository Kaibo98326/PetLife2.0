package com.petlife.service;

import com.petlife.config.ApiException;
import com.petlife.config.BeautyConstants;
import com.petlife.repository.AvailableSlotResponse;
import com.petlife.repository.GroomerResponse;
import com.petlife.model.BeautyTimeSlot;
import com.petlife.repository.BeautyTimeSlotRepository;
import com.petlife.repository.GroomerBeautyItemRepository;
import com.petlife.repository.GroomerProfileRepository;
import com.petlife.repository.GroomerScheduleRepository;
import com.petlife.repository.GroomerWorkSlotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BeautyAvailabilityService {

    private final GroomerProfileRepository groomerRepository;
    private final GroomerBeautyItemRepository gbiRepository;
    private final GroomerScheduleRepository scheduleRepository;
    private final BeautyTimeSlotRepository slotRepository;
    private final GroomerWorkSlotRepository workSlotRepository;

    public BeautyAvailabilityService(
            GroomerProfileRepository groomerRepository,
            GroomerBeautyItemRepository gbiRepository,
            GroomerScheduleRepository scheduleRepository,
            BeautyTimeSlotRepository slotRepository,
            GroomerWorkSlotRepository workSlotRepository) {
        this.groomerRepository = groomerRepository;
        this.gbiRepository = gbiRepository;
        this.scheduleRepository = scheduleRepository;
        this.slotRepository = slotRepository;
        this.workSlotRepository = workSlotRepository;
    }

    public List<GroomerResponse> findAvailableGroomers(List<Integer> beautyIds, LocalDate date) {
        if (beautyIds == null || beautyIds.isEmpty()) {
            return List.of();
        }

        int required = (int) beautyIds.stream().distinct().count();

        return groomerRepository.findByIsBookableTrueOrderByGroomerIdAsc()
                .stream()
                .filter(groomer -> isWorking(groomer.getGroomerId(), date))
                .filter(groomer -> gbiRepository.countActiveServices(groomer.getGroomerId(), beautyIds) == required)
                .map(BeautyMapper::groomer)
                .toList();
    }

    public List<AvailableSlotResponse> findAvailableStartSlots(Integer groomerId, LocalDate date, Integer totalSlots) {
        if (totalSlots == null || totalSlots <= 0) {
            throw ApiException.badRequest("totalSlots 必須大於 0");
        }

        if (!isWorking(groomerId, date)) {
            return List.of();
        }

        List<BeautyTimeSlot> allSlots = slotRepository.findAllByOrderBySortOrderAsc();
        Set<Integer> occupiedSlotIds = workSlotRepository.findByGroomerIdAndWorkDate(groomerId, date)
                .stream()
                .map(workSlot -> workSlot.getSlotId())
                .collect(Collectors.toSet());

        List<BeautyTimeSlot> startSlots = new ArrayList<>();

        for (int i = 0; i <= allSlots.size() - totalSlots; i++) {
            List<BeautyTimeSlot> window = allSlots.subList(i, i + totalSlots);
            if (isWindowAvailable(window, occupiedSlotIds)) {
                startSlots.add(window.get(0));
            }
        }

        return startSlots.stream()
                .map(BeautyMapper::slot)
                .toList();
    }

    public List<BeautyTimeSlot> resolveContinuousSlots(Integer startSlotId, Integer totalSlots) {
        List<BeautyTimeSlot> allSlots = slotRepository.findAllByOrderBySortOrderAsc();
        int startIndex = -1;

        for (int i = 0; i < allSlots.size(); i++) {
            if (Objects.equals(allSlots.get(i).getSlotId(), startSlotId)) {
                startIndex = i;
                break;
            }
        }

        if (startIndex < 0 || startIndex + totalSlots > allSlots.size()) {
            throw ApiException.badRequest("起始時段不合法");
        }

        List<BeautyTimeSlot> window = allSlots.subList(startIndex, startIndex + totalSlots);

        if (!isWindowAvailable(window, Set.of())) {
            throw ApiException.badRequest("起始時段後方沒有足夠連續可預約時段");
        }

        return window;
    }

    private boolean isWorking(Integer groomerId, LocalDate date) {
        return scheduleRepository.findByGroomerIdAndWorkDate(groomerId, date)
                .map(schedule -> BeautyConstants.SCHEDULE_WORK.equals(schedule.getScheduleStatus()))
                .orElse(false);
    }

    private boolean isWindowAvailable(List<BeautyTimeSlot> window, Set<Integer> occupiedSlotIds) {
        for (int i = 0; i < window.size(); i++) {
            BeautyTimeSlot current = window.get(i);

            if (!Boolean.TRUE.equals(current.getIsBookable()) || occupiedSlotIds.contains(current.getSlotId())) {
                return false;
            }

            if (i > 0) {
                BeautyTimeSlot previous = window.get(i - 1);

                if (!Objects.equals(current.getSortOrder(), previous.getSortOrder() + 1)) {
                    return false;
                }

                if (!Objects.equals(previous.getEndTime(), current.getStartTime())) {
                    return false;
                }
            }
        }

        return true;
    }
}
