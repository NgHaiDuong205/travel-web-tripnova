package com.duong.travelweb.converter;

import com.duong.travelweb.model.dto.HotelBookingDTO;
import com.duong.travelweb.model.entity.HotelBookingEntity;
import com.duong.travelweb.model.entity.PaymentEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HotelBookingDTOConverter {
    private final long holdMinutes;

    public HotelBookingDTOConverter(@Value("${app.booking.hold-minutes:15}") long holdMinutes) {
        this.holdMinutes = holdMinutes;
    }

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
        // Khớp HotelBookingServiceImpl.expirePendingBookings (tính từ lúc tạo booking)
        if ("pending".equals(booking.getStatus()) && booking.getCreatedAt() != null) {
            dto.setHoldExpiresAt(booking.getCreatedAt().plusMinutes(holdMinutes));
        }
        if (payment != null) {
            dto.setPaymentId(payment.getId());
            dto.setPaymentMethod(payment.getPaymentMethod());
            dto.setPaymentStatus(payment.getStatus());
            dto.setPaidAt(payment.getPaidAt());
        }
        dto.setCancellable(cancellable);
        if (booking.getUser() != null) {
            dto.setUserId(booking.getUser().getId());
            dto.setUserEmail(booking.getUser().getEmail());
            dto.setUserFullName(booking.getUser().getFullName());
        }
        return dto;
    }
}
