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
        Pet pet = petRepository.findById(req.petId())
                .orElseThrow(() -> ApiException.notFound("找不到寵物"));

        if (!Objects.equals(pet.getMemberId(), memberId)) {
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

        for (Integer beautyId : beautyIds) {
            BeautyItem item = itemMap.get(beautyId);

            if (!Boolean.TRUE.equals(item.getIsActive())) {
                throw ApiException.badRequest("美容項目已停用：" + item.getItemName());
            }

            BeautyItemPrice price = priceRepository
                    .findByBeautyIdAndPetSizeAndIsActiveTrue(beautyId, pet.getPetSize())
                    .orElseThrow(() -> ApiException.badRequest("找不到項目價格：" + item.getItemName()));

            priceMap.put(beautyId, price);
            totalSlots += item.getDurationSlots();
            totalAmount = totalAmount.add(price.getItemPrice());
        }

        List<BeautyTimeSlot> occupiedSlots = availabilityService.resolveContinuousSlots(req.startSlotId(), totalSlots);
        List<Integer> occupiedSlotIds = occupiedSlots.stream()
                .map(BeautyTimeSlot::getSlotId)
                .toList();

        if (!workSlotRepository
                .findByGroomerIdAndWorkDateAndSlotIdIn(req.groomerId(), req.appointDate(), occupiedSlotIds).isEmpty()) {
            throw ApiException.badRequest("選取時段已被預約或封鎖");
        }

        boolean available = availabilityService.findAvailableStartSlots(req.groomerId(), req.appointDate(), totalSlots)
                .stream()
                .anyMatch(slot -> Objects.equals(slot.slotId(), req.startSlotId()));

        if (!available) {
            throw ApiException.badRequest("此起始時段不可預約");
        }

        BeautyAppointment appointment = new BeautyAppointment();
        appointment.setMemberId(memberId);
        appointment.setPetId(req.petId());
        appointment.setGroomerId(req.groomerId());
        appointment.setAppointDate(req.appointDate());
        appointment.setStartSlotId(req.startSlotId());
        appointment.setPetSizeSnapshot(pet.getPetSize());
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

        BeautyAppointment savedAppointment = appointment;
        workSlotRepository.saveAll(occupiedSlots.stream().map(slot -> {
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
        BeautyAppointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("找不到預約單"));

        if (!Objects.equals(appointment.getMemberId(), memberId)) {
            throw ApiException.forbidden("不可取消其他會員預約單");
        }

        cancel(appointment, req == null ? null : req.cancelReason());
        return toResponse(appointment);
    }

    @Transactional
    public void cancel(BeautyAppointment appointment, String reason) {
        String status = appointment.getAppointmentStatus();

        if (BeautyConstants.APPOINTMENT_DONE.equals(status)
                || BeautyConstants.APPOINTMENT_CANCELLED.equals(status)
                || BeautyConstants.APPOINTMENT_NO_SHOW.equals(status)) {
            throw ApiException.badRequest("目前狀態不可取消");
        }

        appointment.setAppointmentStatus(BeautyConstants.APPOINTMENT_CANCELLED);
        appointment.setCancelReason(reason);
        appointmentRepository.save(appointment);
        workSlotRepository.deleteByAppointmentId(appointment.getAppointmentId());
    }

    public AppointmentResponse toResponse(BeautyAppointment appointment) {
        Pet pet = petRepository.findById(appointment.getPetId()).orElse(null);
        GroomerProfile groomer = groomerRepository.findById(appointment.getGroomerId()).orElse(null);
        BeautyTimeSlot slot = slotRepository.findById(appointment.getStartSlotId()).orElse(null);
        List<BeautyAppointmentDetail> details = detailRepository
                .findByAppointmentIdOrderByLineNoAsc(appointment.getAppointmentId());

        return BeautyMapper.appointment(appointment, pet, groomer, slot, details);
    }
}
