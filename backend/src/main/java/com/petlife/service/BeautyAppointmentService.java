package com.petlife.service;

import com.petlife.config.ApiException;
import com.petlife.config.BeautyConstants;
import com.petlife.repository.CancelAppointmentRequest;
import com.petlife.repository.CreateBeautyAppointmentRequest;
import com.petlife.repository.AppointmentResponse;
import com.petlife.model.BeautyAppointment;
import com.petlife.model.BeautyAppointmentDetail;
import com.petlife.model.BeautyItem;
import com.petlife.model.BeautyItemPrice;
import com.petlife.model.BeautyTimeSlot;
import com.petlife.model.GroomerProfile;
import com.petlife.model.GroomerWorkSlot;
import com.petlife.model.Pet;
import com.petlife.repository.BeautyAppointmentDetailRepository;
import com.petlife.repository.BeautyAppointmentRepository;
import com.petlife.repository.BeautyItemPriceRepository;
import com.petlife.repository.BeautyItemRepository;
import com.petlife.repository.BeautyTimeSlotRepository;
import com.petlife.repository.GroomerBeautyItemRepository;
import com.petlife.repository.GroomerProfileRepository;
import com.petlife.repository.GroomerWorkSlotRepository;
import com.petlife.repository.PetRepository;
import com.petlife.repository.RescheduleAppointmentRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BeautyAppointmentService {

    private final BeautyAppointmentRepository appointmentRepository;
    private final BeautyAppointmentDetailRepository detailRepository;
    private final BeautyItemRepository itemRepository;
    private final BeautyItemPriceRepository priceRepository;
    private final PetRepository petRepository;
    private final GroomerProfileRepository groomerRepository;
    private final GroomerBeautyItemRepository gbiRepository;
    private final GroomerWorkSlotRepository workSlotRepository;
    private final BeautyTimeSlotRepository slotRepository;
    private final BeautyAvailabilityService availabilityService;

    public BeautyAppointmentService(
            BeautyAppointmentRepository appointmentRepository,
            BeautyAppointmentDetailRepository detailRepository,
            BeautyItemRepository itemRepository,
            BeautyItemPriceRepository priceRepository,
            PetRepository petRepository,
            GroomerProfileRepository groomerRepository,
            GroomerBeautyItemRepository gbiRepository,
            GroomerWorkSlotRepository workSlotRepository,
            BeautyTimeSlotRepository slotRepository,
            BeautyAvailabilityService availabilityService) {
        this.appointmentRepository = appointmentRepository;
        this.detailRepository = detailRepository;
        this.itemRepository = itemRepository;
        this.priceRepository = priceRepository;
        this.petRepository = petRepository;
        this.groomerRepository = groomerRepository;
        this.gbiRepository = gbiRepository;
        this.workSlotRepository = workSlotRepository;
        this.slotRepository = slotRepository;
        this.availabilityService = availabilityService;
    }

    @Transactional
    public AppointmentResponse createAppointment(Integer memberId, CreateBeautyAppointmentRequest req) {
        validateCreateRequest(memberId, req);

        Pet pet = petRepository.findById(req.petId())
                .orElseThrow(() -> ApiException.notFound("找不到寵物"));

        if (pet.getMember() == null || !Objects.equals(pet.getMember().getMemberId(), memberId)) {
            throw ApiException.forbidden("不可替其他會員的寵物預約");
        }

        List<Integer> beautyIds = req.beautyIds().stream().distinct().toList();

        GroomerProfile groomer = groomerRepository.findById(req.groomerId())
                .orElseThrow(() -> ApiException.notFound("找不到美容師"));

        if (!Boolean.TRUE.equals(groomer.getIsBookable())) {
            throw ApiException.badRequest("此美容師目前不可預約");
        }

        if (gbiRepository.countActiveServices(req.groomerId(), beautyIds) != beautyIds.size()) {
            throw ApiException.badRequest("美容師無法服務所有選取項目");
        }

        List<BeautyItem> items = itemRepository.findAllById(beautyIds);
        if (items.size() != beautyIds.size()) {
            throw ApiException.badRequest("美容項目資料不完整");
        }

        Map<Integer, BeautyItem> itemMap = items.stream()
                .collect(Collectors.toMap(BeautyItem::getBeautyId, Function.identity()));

        Map<Integer, BeautyItemPrice> priceMap = new HashMap<>();
        int totalSlots = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        String petSize = resolvePetSize(pet);

        for (Integer beautyId : beautyIds) {
            BeautyItem item = itemMap.get(beautyId);

            if (!Boolean.TRUE.equals(item.getIsActive())) {
                throw ApiException.badRequest("美容項目已停用：" + item.getItemName());
            }

            BeautyItemPrice price = priceRepository
                    .findByBeautyIdAndPetSizeAndIsActiveTrue(beautyId, petSize)
                    .orElseThrow(() -> ApiException.badRequest("找不到項目價格：" + item.getItemName()));

            if (item.getDurationSlots() == null || item.getDurationSlots() <= 0) {
                throw ApiException.badRequest("美容項目時長設定不正確：" + item.getItemName());
            }
            if (price.getItemPrice() == null) {
                throw ApiException.badRequest("美容項目價格不可為空：" + item.getItemName());
            }

            priceMap.put(beautyId, price);
            totalSlots += item.getDurationSlots();
            totalAmount = totalAmount.add(price.getItemPrice());
        }

        List<BeautyTimeSlot> occupiedSlots = validateAvailableSlots(req.groomerId(), req.appointDate(),
                req.startSlotId(), totalSlots);

        BeautyAppointment appointment = new BeautyAppointment();
        appointment.setMemberId(memberId);
        appointment.setPetId(req.petId());
        appointment.setGroomerId(req.groomerId());
        appointment.setAppointDate(req.appointDate());
        appointment.setStartSlotId(req.startSlotId());
        appointment.setPetSizeSnapshot(petSize);
        appointment.setTotalSlots(totalSlots);
        appointment.setTotalAmount(totalAmount);
        appointment.setAppointmentStatus(BeautyConstants.APPOINTMENT_PENDING);
        appointment.setContactNote(req.contactNote());

        appointment = appointmentRepository.saveAndFlush(appointment);

        int lineNo = 1;
        List<BeautyAppointmentDetail> details = new ArrayList<>();

        for (Integer beautyId : beautyIds) {
            BeautyItem item = itemMap.get(beautyId);
            BeautyItemPrice price = priceMap.get(beautyId);

            BeautyAppointmentDetail detail = new BeautyAppointmentDetail();
            detail.setAppointmentId(appointment.getAppointmentId());
            detail.setLineNo(lineNo++);
            detail.setBeautyId(beautyId);
            detail.setItemNameSnapshot(item.getItemName());
            detail.setItemPriceSnapshot(price.getItemPrice());
            detail.setDurationSlotsSnapshot(item.getDurationSlots());
            details.add(detail);
        }

        detailRepository.saveAll(details);

        assertSlotsStillAvailable(req.groomerId(), req.appointDate(), occupiedSlots);
        BeautyAppointment savedAppointment = appointment;
        workSlotRepository.saveAllAndFlush(occupiedSlots.stream().map(slot -> {
            GroomerWorkSlot workSlot = new GroomerWorkSlot();
            workSlot.setGroomerId(req.groomerId());
            workSlot.setWorkDate(req.appointDate());
            workSlot.setSlotId(slot.getSlotId());
            workSlot.setAppointmentId(savedAppointment.getAppointmentId());
            workSlot.setWorkSlotStatus(BeautyConstants.WORK_SLOT_APPOINTMENT);
            return workSlot;
        }).toList());

        return toResponse(appointment);
    }

    public List<AppointmentResponse> getMemberAppointments(Integer memberId) {
        return appointmentRepository.findByMemberIdOrderByAppointDateDescAppointmentIdDesc(memberId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AppointmentResponse getMemberAppointment(Integer memberId, Integer id) {
        BeautyAppointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("找不到預約單"));

        if (!Objects.equals(appointment.getMemberId(), memberId)) {
            throw ApiException.forbidden("不可查看其他會員預約單");
        }

        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse cancelByMember(Integer memberId, Integer id, CancelAppointmentRequest req) {
        if (memberId == null) {
            throw ApiException.badRequest("會員編號不可為空");
        }
        if (id == null) {
            throw ApiException.badRequest("預約單編號不可為空");
        }

        BeautyAppointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("找不到預約單"));

        if (!Objects.equals(appointment.getMemberId(), memberId)) {
            throw ApiException.forbidden("不可取消其他會員預約單");
        }

        cancel(appointment, req == null ? null : req.cancelReason());
        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse rescheduleByMember(Integer memberId, Integer id, RescheduleAppointmentRequest req) {
        validateRescheduleRequest(memberId, id, req);

        BeautyAppointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("找不到預約單"));

        if (!Objects.equals(appointment.getMemberId(), memberId)) {
            throw ApiException.forbidden("不可改期其他會員預約單");
        }

        validateReschedulableStatus(appointment.getAppointmentStatus());

        if (Objects.equals(appointment.getAppointDate(), req.appointDate())
                && Objects.equals(appointment.getStartSlotId(), req.startSlotId())) {
            return toResponse(appointment);
        }

        workSlotRepository.deleteByAppointmentIdAndWorkSlotStatus(appointment.getAppointmentId(),
                BeautyConstants.WORK_SLOT_APPOINTMENT);
        workSlotRepository.flush();

        List<BeautyTimeSlot> occupiedSlots = validateAvailableSlots(appointment.getGroomerId(), req.appointDate(),
                req.startSlotId(), appointment.getTotalSlots());

        assertSlotsStillAvailable(appointment.getGroomerId(), req.appointDate(), occupiedSlots);

        appointment.setAppointDate(req.appointDate());
        appointment.setStartSlotId(req.startSlotId());
        BeautyAppointment savedAppointment = appointmentRepository.save(appointment);

        workSlotRepository.saveAllAndFlush(occupiedSlots.stream().map(slot -> {
            GroomerWorkSlot workSlot = new GroomerWorkSlot();
            workSlot.setGroomerId(savedAppointment.getGroomerId());
            workSlot.setWorkDate(savedAppointment.getAppointDate());
            workSlot.setSlotId(slot.getSlotId());
            workSlot.setAppointmentId(savedAppointment.getAppointmentId());
            workSlot.setWorkSlotStatus(BeautyConstants.WORK_SLOT_APPOINTMENT);
            return workSlot;
        }).toList());

        return toResponse(savedAppointment);
    }

    @Transactional
    public void cancel(BeautyAppointment appointment, String reason) {
        if (appointment == null || appointment.getAppointmentId() == null) {
            throw ApiException.badRequest("預約單資料不可為空");
        }

        String status = appointment.getAppointmentStatus();

        if (BeautyConstants.APPOINTMENT_DONE.equals(status)
                || BeautyConstants.APPOINTMENT_CANCELLED.equals(status)
                || BeautyConstants.APPOINTMENT_NO_SHOW.equals(status)) {
            throw ApiException.badRequest("目前狀態不可取消");
        }

        appointment.setAppointmentStatus(BeautyConstants.APPOINTMENT_CANCELLED);
        appointment.setCancelReason(reason);
        appointmentRepository.save(appointment);
        workSlotRepository.deleteByAppointmentIdAndWorkSlotStatus(appointment.getAppointmentId(),
                BeautyConstants.WORK_SLOT_APPOINTMENT);
    }

    public AppointmentResponse toResponse(BeautyAppointment appointment) {
        Pet pet = petRepository.findById(appointment.getPetId()).orElse(null);
        GroomerProfile groomer = groomerRepository.findById(appointment.getGroomerId()).orElse(null);
        BeautyTimeSlot slot = slotRepository.findById(appointment.getStartSlotId()).orElse(null);
        List<BeautyAppointmentDetail> details = detailRepository
                .findByAppointmentIdOrderByLineNoAsc(appointment.getAppointmentId());

        return BeautyMapper.appointment(appointment, pet, groomer, slot, details);
    }

    private String resolvePetSize(Pet pet) {
        Double weight = pet.getWeight();
        if (weight == null) {
            throw ApiException.badRequest("寵物體重不可為空，無法判斷美容價格級距");
        }
        if (weight <= 10) {
            return "小型";
        }
        if (weight <= 20) {
            return "中型";
        }
        return "大型";
    }

    private void validateCreateRequest(Integer memberId, CreateBeautyAppointmentRequest req) {
        if (memberId == null) {
            throw ApiException.badRequest("會員編號不可為空");
        }
        if (req == null) {
            throw ApiException.badRequest("預約資料不可為空");
        }
        if (req.petId() == null) {
            throw ApiException.badRequest("寵物編號不可為空");
        }
        if (req.groomerId() == null) {
            throw ApiException.badRequest("美容師編號不可為空");
        }
        if (req.appointDate() == null) {
            throw ApiException.badRequest("預約日期不可為空");
        }
        if (req.startSlotId() == null) {
            throw ApiException.badRequest("起始時段不可為空");
        }
        if (req.beautyIds() == null || req.beautyIds().isEmpty()) {
            throw ApiException.badRequest("至少需選擇一個美容項目");
        }
        if (req.beautyIds().stream().anyMatch(Objects::isNull)) {
            throw ApiException.badRequest("美容項目編號不可為空");
        }
    }

    private void validateRescheduleRequest(Integer memberId, Integer id, RescheduleAppointmentRequest req) {
        if (memberId == null) {
            throw ApiException.badRequest("會員編號不可為空");
        }
        if (id == null) {
            throw ApiException.badRequest("預約單編號不可為空");
        }
        if (req == null) {
            throw ApiException.badRequest("改期資料不可為空");
        }
        if (req.appointDate() == null) {
            throw ApiException.badRequest("新預約日期不可為空");
        }
        if (req.startSlotId() == null) {
            throw ApiException.badRequest("新起始時段不可為空");
        }
    }

    private void validateReschedulableStatus(String status) {
        if (!BeautyConstants.APPOINTMENT_PENDING.equals(status)
                && !BeautyConstants.APPOINTMENT_CONFIRMED.equals(status)) {
            throw ApiException.badRequest("目前狀態不可改期");
        }
    }

    private List<BeautyTimeSlot> validateAvailableSlots(Integer groomerId, java.time.LocalDate appointDate,
            Integer startSlotId, Integer totalSlots) {
        if (totalSlots == null || totalSlots <= 0) {
            throw ApiException.badRequest("預約占用時段數不正確");
        }

        List<BeautyTimeSlot> occupiedSlots = availabilityService.resolveContinuousSlots(startSlotId, totalSlots);
        List<Integer> occupiedSlotIds = occupiedSlots.stream()
                .map(BeautyTimeSlot::getSlotId)
                .toList();

        if (workSlotRepository.existsByGroomerIdAndWorkDateAndSlotIdIn(groomerId, appointDate, occupiedSlotIds)) {
            throw ApiException.badRequest("選取時段已被預約或關閉");
        }

        boolean available = availabilityService.findAvailableStartSlots(groomerId, appointDate, totalSlots)
                .stream()
                .anyMatch(slot -> Objects.equals(slot.slotId(), startSlotId));

        if (!available) {
            throw ApiException.badRequest("此起始時段不可預約");
        }

        return occupiedSlots;
    }

    private void assertSlotsStillAvailable(Integer groomerId, java.time.LocalDate appointDate,
            List<BeautyTimeSlot> occupiedSlots) {
        List<Integer> occupiedSlotIds = occupiedSlots.stream()
                .map(BeautyTimeSlot::getSlotId)
                .toList();

        if (workSlotRepository.existsByGroomerIdAndWorkDateAndSlotIdIn(groomerId, appointDate, occupiedSlotIds)) {
            throw ApiException.badRequest("選取時段已被預約或關閉");
        }
    }
}
