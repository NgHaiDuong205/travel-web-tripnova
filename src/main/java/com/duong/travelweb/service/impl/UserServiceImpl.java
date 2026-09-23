package com.duong.travelweb.service.impl;

import com.duong.travelweb.converter.UserDTOConverter;
import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.DashboardDTO;
import com.duong.travelweb.model.dto.HotelBookingDTO;
import com.duong.travelweb.model.dto.ProfileUpdateRequestDTO;
import com.duong.travelweb.model.dto.UserDTO;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.repository.HotelBookingRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.service.HotelBookingService;
import com.duong.travelweb.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    private static final int RECENT_BOOKINGS_LIMIT = 5;

    private final UserRepository userRepository;
    private final HotelBookingRepository hotelBookingRepository;
    private final HotelBookingService hotelBookingService;
    private final UserDTOConverter userDTOConverter;
    private final String currencyCode;

    public UserServiceImpl(UserRepository userRepository,
                           HotelBookingRepository hotelBookingRepository,
                           HotelBookingService hotelBookingService,
                           UserDTOConverter userDTOConverter,
                           @Value("${app.booking.currency:USD}") String currencyCode) {
        this.userRepository = userRepository;
        this.hotelBookingRepository = hotelBookingRepository;
        this.hotelBookingService = hotelBookingService;
        this.userDTOConverter = userDTOConverter;
        this.currencyCode = currencyCode;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getProfile(UUID userId) {
        UserEntity user = findUser(userId);
        return userDTOConverter.toUserDTO(user, userRepository.findRoleNamesByUserId(userId));
    }

    @Override
    @Transactional
    public UserDTO updateProfile(UUID userId, ProfileUpdateRequestDTO request) {
        UserEntity user = findUser(userId);
        user.setFullName(request.getFullName().trim());
        user.setPhone(blankToNull(request.getPhone()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(blankToNull(request.getGender()));
        user.setAvatarUrl(blankToNull(request.getAvatarUrl()));
        user.setUpdatedAt(LocalDateTime.now());
        return userDTOConverter.toUserDTO(user, userRepository.findRoleNamesByUserId(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardDTO getDashboard(UUID userId) {
        UserEntity user = findUser(userId);
        DashboardDTO dto = new DashboardDTO();
        dto.setTotalBookings(hotelBookingService.countMyBookings(userId, "all"));
        dto.setUpcomingBookings(hotelBookingService.countMyBookings(userId, "upcoming"));
        dto.setCompletedBookings(hotelBookingService.countMyBookings(userId, "completed"));
        dto.setCancelledBookings(hotelBookingService.countMyBookings(userId, "cancelled"));
        dto.setTotalSpent(hotelBookingRepository.sumSpentByUser(userId));
        dto.setCurrencyCode(currencyCode);
        dto.setLoyaltyPoints(user.getLoyaltyPoints());
        dto.setRecentBookings(hotelBookingService.findMyBookings(userId, "all", 1, RECENT_BOOKINGS_LIMIT));

        HotelBookingDTO next = hotelBookingService.findNextUpcoming(userId);
        dto.setNextBooking(next);
        if (next != null) {
            dto.setDaysUntilNextTrip(Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), next.getCheckInDate())));
        }
        return dto;
    }

    private UserEntity findUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy người dùng"));
        if (user.getDeletedAt() != null) {
            throw ApiException.notFound("Không tìm thấy người dùng");
        }
        return user;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
