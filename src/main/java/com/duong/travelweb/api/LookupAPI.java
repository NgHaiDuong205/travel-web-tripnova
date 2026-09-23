package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.AmenityDTO;
import com.duong.travelweb.model.dto.ContinentDTO;
import com.duong.travelweb.model.dto.RoomTypeDTO;
import com.duong.travelweb.service.AmenityService;
import com.duong.travelweb.service.ContinentService;
import com.duong.travelweb.service.RoomTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Dữ liệu tra cứu công khai: châu lục, tiện nghi, hạng phòng. */
@RestController
public class LookupAPI {
    private final ContinentService continentService;
    private final AmenityService amenityService;
    private final RoomTypeService roomTypeService;

    public LookupAPI(ContinentService continentService, AmenityService amenityService, RoomTypeService roomTypeService) {
        this.continentService = continentService;
        this.amenityService = amenityService;
        this.roomTypeService = roomTypeService;
    }

    @GetMapping("/api/continents/")
    public ResponseEntity<List<ContinentDTO>> getContinents() {
        return ResponseEntity.ok(continentService.findAll());
    }

    @GetMapping("/api/amenities/")
    public ResponseEntity<List<AmenityDTO>> getAmenities(@RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(amenityService.findAmenities(category));
    }

    @GetMapping("/api/room-types/")
    public ResponseEntity<List<RoomTypeDTO>> getRoomTypes(@RequestParam("hotelId") UUID hotelId) {
        return ResponseEntity.ok(roomTypeService.findByHotelId(hotelId));
    }

    @GetMapping("/api/hotels/{hotelId}/room-types/")
    public ResponseEntity<List<RoomTypeDTO>> getHotelRoomTypes(@PathVariable("hotelId") UUID hotelId) {
        return ResponseEntity.ok(roomTypeService.findByHotelId(hotelId));
    }
}
