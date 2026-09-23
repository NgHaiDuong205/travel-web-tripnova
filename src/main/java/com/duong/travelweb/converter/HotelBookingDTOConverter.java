package com.duong.travelweb.converter;

import com.duong.travelweb.model.dto.HotelBookingDTO;
import com.duong.travelweb.model.entity.HotelBookingEntity;
import com.duong.travelweb.model.entity.PaymentEntity;
import org.springframework.stereotype.Component;

@Component
public class HotelBookingDTOConverter {

    public HotelBookingDTO toHotelBookingDTO(HotelBookingEntity booking, PaymentEntity payment, boolean cancellable) {
        HotelBookingDTO dto = new HotelBookingDTO();
        dto.setId(booking.getId());
        dto.setStatus(booking.getStatus());
        dto.setOrderId(booking.getOrder().getId());
        dto.setOrderCode(booking.getOrder().getOrderCode());
        dto.setOrderStatus(booking.getOrder().getStatus());
        dto.setCurrencyCode(booking.getOrder().getCurrencyCode());
        dto.setHotelId(booking.getHotel().getId());
        dto.setHotelName(booking.getHotel().getName());
        dto.setHotelAddress(booking.getHotel().getAddress());
        dto.setHotelCoverImageUrl(booking.getHotel().getCoverImageUrl());
        dto.setRoomTypeId(booking.getRoomType().getId());
        dto.setRoomTypeName(booking.getRoomType().getName());
        if (booking.getRoom() != null) {
            dto.setRoomId(booking.getRoom().getId());
            dto.setRoomNumber(booking.getRoom().getRoomNumber());
        }
        dto.setCheckInDate(booking.getCheckInDate());
        dto.setCheckOutDate(booking.getCheckOutDate());
        dto.setNumNights(booking.getNumNights());
        dto.setNumAdults(booking.getNumAdults());
        dto.setNumChildren(booking.getNumChildren());
        dto.setTotalPrice(booking.getTotalPrice());
        dto.setDiscountAmount(booking.getDiscountAmount());
        dto.setSpecialRequests(booking.getSpecialRequests());
        dto.setRefundAmount(booking.getRefundAmount());
        dto.setRefundReason(booking.getRefundReason());
        dto.setRefundedAt(booking.getRefundedAt());
        dto.setCreatedAt(booking.getCreatedAt());
        if (payment != null) {
            dto.setPaymentId(payment.getId());
            dto.setPaymentMethod(payment.getPaymentMethod());
            dto.setPaymentStatus(payment.getStatus());
            dto.setPaidAt(payment.getPaidAt());
        }
        dto.setCancellable(cancellable);
        return dto;
    }
}
