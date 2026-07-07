package com.seatwise.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.seatwise.common.R;
import com.seatwise.service.ScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    @SaCheckLogin
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        return R.ok(scoreService.me(Long.valueOf(StpUtil.getLoginId().toString())));
    }

    @SaCheckLogin
    @GetMapping("/ranking")
    public R<List<Map<String, Object>>> ranking(@RequestParam(required = false) String period) {
        return R.ok(scoreService.ranking());
    }
}
