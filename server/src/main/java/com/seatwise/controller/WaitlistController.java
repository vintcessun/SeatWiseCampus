package com.seatwise.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.seatwise.common.R;
import com.seatwise.entity.Waitlist;
import com.seatwise.service.WaitlistService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;

    private Long uid() { return Long.valueOf(StpUtil.getLoginId().toString()); }

    @SaCheckRole("STUDENT")
    @PostMapping
    public R<Waitlist> join(@RequestBody WaitReq req) {
        return R.ok(waitlistService.join(uid(), req.getRoomId(), req.getDate(), req.getStartTime(), req.getEndTime()));
    }

    @SaCheckRole("STUDENT")
    @GetMapping("/me")
    public R<List<Waitlist>> mine() {
        return R.ok(waitlistService.listMine(uid()));
    }

    @SaCheckRole("STUDENT")
    @PostMapping("/{id}/accept")
    public R<Void> accept(@PathVariable Long id) {
        waitlistService.accept(uid(), id);
        return R.ok();
    }

    @SaCheckRole("STUDENT")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        waitlistService.cancel(uid(), id);
        return R.ok();
    }

    @Data
    public static class WaitReq {
        private Long roomId;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;
    }
}
