package com.seatwise.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.seatwise.common.R;
import com.seatwise.service.NearbyService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class NearbyController {

    private final NearbyService nearbyService;

    @SaCheckLogin
    @GetMapping("/nearest-available")
    public R<List<Map<String, Object>>> nearest(@RequestParam Long originBuildingId,
                                                 @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                                 @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime start,
                                                 @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime end,
                                                 @RequestParam(required = false) Double userLat,
                                                 @RequestParam(required = false) Double userLng) {
        return R.ok(nearbyService.nearestAvailable(originBuildingId, date, start, end, userLat, userLng));
    }

    @SaCheckLogin
    @GetMapping("/alternatives")
    public R<List<Map<String, Object>>> alternatives(@RequestParam Long roomId,
                                                     @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                                     @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime start,
                                                     @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime end,
                                                     @RequestParam(required = false) Long excludeSeatId) {
        return R.ok(nearbyService.alternatives(roomId, date, start, end, excludeSeatId));
    }
}
