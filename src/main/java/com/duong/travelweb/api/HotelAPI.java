package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.HotelDTO;
import com.duong.travelweb.model.dto.RoomDTO;
import com.duong.travelweb.service.HotelService;
import com.duong.travelweb.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class HotelAPI {
    @Autowired
    private HotelService hotelService;

    @Autowired
    private RoomService roomService;

    @GetMapping("/api/hotels/")
    public ResponseEntity<List<HotelDTO>> getHotel (@RequestParam Map<String,Object> params,
                                                    @RequestParam(required = false) List<String> amenities){
        List<HotelDTO> results = hotelService.findHotel(params, amenities);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/api/hotels/{id}/")
    public ResponseEntity<HotelDTO> getHotelById(@PathVariable UUID id) {
        HotelDTO result = hotelService.getHotelById(id);
        if (result != null && result.getId() != null) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @GetMapping("/api/hotels/{hotelId}/rooms/")
    public ResponseEntity<List<RoomDTO>> getRoomsByHotelId(@PathVariable UUID hotelId,
                                                           @RequestParam Map<String, Object> params,
                                                           @RequestParam(required = false) List<String> amenities) {
        List<RoomDTO> results = roomService.findRoomsByHotelId(hotelId, params, amenities);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/api/hotels/{hotelId}/rooms/{roomId}/")
    public ResponseEntity<RoomDTO> getRoomByIdAndHotelId(@PathVariable UUID hotelId,
                                                         @PathVariable UUID roomId) {
        RoomDTO result = roomService.findRoomByIdAndHotelId(hotelId, roomId);
        if (result != null) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
