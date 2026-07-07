package com.seatwise.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.seatwise.common.R;
import com.seatwise.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @SaCheckRole("ADMIN")
    @GetMapping("/summary")
    public R<Map<String, Object>> summary() {
        return R.ok(reportService.summary());
    }
}
