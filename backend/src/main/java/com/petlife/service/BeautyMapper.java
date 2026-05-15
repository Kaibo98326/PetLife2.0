package com.petlife.service;

import com.petlife.repository.AppointmentDetailLineResponse;
import com.petlife.repository.AppointmentResponse;
import com.petlife.repository.AvailableSlotResponse;
import com.petlife.repository.BeautyItemManageResponse;
import com.petlife.repository.BeautyItemResponse;
import com.petlife.repository.BeautyPriceResponse;
import com.petlife.repository.GroomerManageResponse;
import com.petlife.repository.GroomerResponse;
import com.petlife.repository.GroomerScheduleResponse;
import com.petlife.repository.GroomerServiceResponse;
import com.petlife.repository.GroomerWorkSlotResponse;
import com.petlife.model.BeautyAppointment;
import com.petlife.model.BeautyAppointmentDetail;
import com.petlife.model.BeautyItem;
import com.petlife.model.BeautyItemPrice;
import com.petlife.model.BeautyTimeSlot;
import com.petlife.model.GroomerBeautyItem;
import com.petlife.model.GroomerProfile;
import com.petlife.model.GroomerSchedule;
import com.petlife.model.GroomerWorkSlot;
import com.petlife.model.Pet;

import java.util.List;

public class BeautyMapper {

    private BeautyMapper() {
    }

    public static BeautyItemResponse item(BeautyItem item, BeautyItemPrice price) {
        return new BeautyItemResponse(
                item.getBeautyId(),
                item.getItemName(),
                item.getItemDescription(),
                item.getImageUrl(),
                item.getDurationSlots(),
                item.getIsActive(),
                price == null ? null : price.getItemPrice(),
                price == null ? null : price.getPetSize());
    }

    public static BeautyItemResponse item(BeautyItem item) {
        return item(item, null);
    }

    public static BeautyPriceResponse price(BeautyItemPrice price) {
        return new BeautyPriceResponse(
                price.getPriceId(),
                price.getBeautyId(),
                price.getPetSize(),
                price.getItemPrice(),
                price.getIsActive());
    }

    public static BeautyItemManageResponse itemManage(BeautyItem item, List<BeautyItemPrice> prices) {
        return new BeautyItemManageResponse(
                item.getBeautyId(),
                item.getItemName(),
                item.getItemDescription(),
                item.getImageUrl(),
                item.getDurationSlots(),
                item.getIsActive(),
                prices.stream().map(BeautyMapper::price).toList());
    }

    public static GroomerResponse groomer(GroomerProfile groomer) {
        return new GroomerResponse(
                groomer.getGroomerId(),
                groomer.getDisplayName(),
                groomer.getIntro(),
                groomer.getSeniorityYears(),
                groomer.getIsBookable());
    }

    public static GroomerServiceResponse groomerService(GroomerBeautyItem service) {
        return new GroomerServiceResponse(
                service.getGroomerBeautyItemId(),
                service.getGroomerId(),
                service.getBeautyId(),
                service.getIsActive(),
                service.getNote());
    }

    public static GroomerManageResponse groomerManage(GroomerProfile groomer, List<GroomerServiceResponse> services) {
        return new GroomerManageResponse(
                groomer.getGroomerId(),
                groomer.getDisplayName(),
                groomer.getIntro(),
                groomer.getSeniorityYears(),
                groomer.getIsBookable(),
                services);
    }

    public static GroomerScheduleResponse schedule(GroomerSchedule schedule) {
        return new GroomerScheduleResponse(
                schedule.getScheduleId(),
                schedule.getGroomerId(),
                schedule.getWorkDate(),
                schedule.getScheduleStatus(),
                schedule.getNote());
    }

    public static GroomerWorkSlotResponse workSlot(GroomerWorkSlot workSlot) {
        return new GroomerWorkSlotResponse(
                workSlot.getWorkSlotId(),
                workSlot.getGroomerId(),
                workSlot.getWorkDate(),
                workSlot.getSlotId(),
                workSlot.getAppointmentId(),
                workSlot.getWorkSlotStatus(),
                workSlot.getNote());
    }

    public static AvailableSlotResponse slot(BeautyTimeSlot slot) {
        return new AvailableSlotResponse(
                slot.getSlotId(),
                slot.getSlotName(),
                slot.getStartTime().toString(),
                slot.getEndTime().toString(),
                slot.getSortOrder());
    }

    public static AppointmentDetailLineResponse line(BeautyAppointmentDetail detail) {
        return new AppointmentDetailLineResponse(
                detail.getDetailId(),
                detail.getLineNo(),
                detail.getBeautyId(),
                detail.getItemNameSnapshot(),
                detail.getItemPriceSnapshot(),
                detail.getDurationSlotsSnapshot(),
                detail.getLineNote());
    }

    public static AppointmentResponse appointment(
            BeautyAppointment appointment,
            Pet pet,
            GroomerProfile groomer,
            BeautyTimeSlot slot,
            List<BeautyAppointmentDetail> details) {
        return new AppointmentResponse(
                appointment.getAppointmentId(),
                appointment.getMemberId(),
                appointment.getPetId(),
                pet == null ? null : pet.getPetName(),
                appointment.getGroomerId(),
                groomer == null ? null : groomer.getDisplayName(),
                appointment.getAppointDate(),
                appointment.getStartSlotId(),
                slot == null ? null : slot.getSlotName(),
                appointment.getPetSizeSnapshot(),
                appointment.getTotalSlots(),
                appointment.getTotalAmount(),
                appointment.getAppointmentStatus(),
                appointment.getContactNote(),
                appointment.getCancelReason(),
                appointment.getCreatedAt(),
                details.stream().map(BeautyMapper::line).toList());
    }
}
