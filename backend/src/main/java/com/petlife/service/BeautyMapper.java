package com.petlife.service;

import com.petlife.repository.AppointmentDetailLineResponse;
import com.petlife.repository.AppointmentResponse;
import com.petlife.repository.AvailableSlotResponse;
import com.petlife.repository.BeautyItemResponse;
import com.petlife.repository.GroomerResponse;
import com.petlife.model.BeautyAppointment;
import com.petlife.model.BeautyAppointmentDetail;
import com.petlife.model.BeautyItem;
import com.petlife.model.BeautyItemPrice;
import com.petlife.model.BeautyTimeSlot;
import com.petlife.model.GroomerProfile;
import com.petlife.beauty.entity.Pet;

import java.util.List;

public class BeautyMapper {

    private BeautyMapper() {
    }

    public static BeautyItemResponse item(BeautyItem item, BeautyItemPrice price) {
        return new BeautyItemResponse(
                item.getBeautyId(),
                item.getItemName(),
                item.getItemDescription(),
                item.getDurationSlots(),
                item.getIsActive(),
                price == null ? null : price.getItemPrice(),
                price == null ? null : price.getPetSize());
    }

    public static GroomerResponse groomer(GroomerProfile groomer) {
        return new GroomerResponse(
                groomer.getGroomerId(),
                groomer.getDisplayName(),
                groomer.getIntro(),
                groomer.getSeniorityYears(),
                groomer.getIsBookable());
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
