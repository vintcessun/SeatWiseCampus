package com.seatwise.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.seatwise.common.R;
import com.seatwise.service.HoldService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/holds")
@RequiredArgsConstructor
public class HoldController {

    private final HoldService holdService;

    private Long uid() { return Long.valueOf(StpUtil.getLoginId().toString()); }

    @SaCheckRole("STUDENT")
    @PostMapping
    public R<Map<String, Object>> hold(@RequestBody HoldReq req) {
        long expireAt = holdService.hold(uid(), req.getRoomId(), req.getSeatId(), req.getDate(),
                req.getStartTime(), req.getEndTime());
        return R.ok(Map.of("expireAt", expireAt, "holdSeconds", (expireAt - System.currentTimeMillis()) / 1000));
    }

    @SaCheckRole("STUDENT")
    @PostMapping("/release")
    public R<Void> release(@RequestBody HoldReq req) {
        holdService.release(uid(), req.getRoomId(), req.getSeatId(), req.getDate());
        return R.ok();
    }

    @Data
    public static class HoldReq {
        private Long roomId;
        private Long seatId;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;
    }
}
