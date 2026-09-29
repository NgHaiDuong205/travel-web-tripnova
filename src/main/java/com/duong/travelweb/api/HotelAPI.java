package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.HotelDTO;
import com.duong.travelweb.model.dto.RoomDTO;
import com.duong.travelweb.service.HotelService;
import com.duong.travelweb.service.RoomService;
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
    private final HotelService hotelService;
    private final RoomService roomService;

    public HotelAPI(HotelService hotelService, RoomService roomService) {
        this.hotelService = hotelService;
        this.roomService = roomService;
    }

    @GetMapping("/api/hotels/")
    public ResponseEntity<List<HotelDTO>> getHotel (@RequestParam Map<String,Object> params,
                                                    @RequestParam(value = "amenities", required = false) List<String> amenities){
        List<HotelDTO> results = hotelService.findHotel(params, amenities);
        long total = hotelService.countHotel(params, amenities);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(total))
                .body(results);
    }

    @GetMapping("/api/hotels/{id}/")
    public ResponseEntity<HotelDTO> getHotelById(@PathVariable("id") UUID id,
                                                 @RequestParam Map<String, Object> params) {
        return ResponseEntity.ok(hotelService.getHotelById(id, params));
    }

    @GetMapping("/api/hotels/{hotelId}/rooms/")
    public ResponseEntity<List<RoomDTO>> getRoomsByHotelId(@PathVariable("hotelId") UUID hotelId,
                                                           @RequestParam Map<String, Object> params,
                                                           @RequestParam(value = "amenities", required = false) List<String> amenities) {
        List<RoomDTO> results = roomService.findRoomsByHotelId(hotelId, params, amenities);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/api/hotels/{hotelId}/rooms/{roomId}/")
    public ResponseEntity<RoomDTO> getRoomByIdAndHotelId(@PathVariable("hotelId") UUID hotelId,
                                                         @PathVariable("roomId") UUID roomId) {
        return ResponseEntity.ok(roomService.findRoomByIdAndHotelId(hotelId, roomId));
    }
}
