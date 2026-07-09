package com.seatwise.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.seatwise.common.R;
import com.seatwise.service.StudyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final StudyReportService studyReportService;

    @SaCheckLogin
    @GetMapping("/study-report")
    public R<Map<String, Object>> studyReport() {
        return R.ok(studyReportService.report(Long.valueOf(StpUtil.getLoginId().toString())));
    }
}
