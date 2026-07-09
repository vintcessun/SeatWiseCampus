package com.seatwise.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.seatwise.common.R;
import com.seatwise.service.AiService;
import com.seatwise.vo.AiReplyVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @SaCheckLogin
    @PostMapping("/assistant")
    public R<AiReplyVO> assistant(@RequestBody AiAsk ask) {
        return R.ok(aiService.assistant(ask.getMessage(), ask.getCampusId(), ask.getDate()));
    }

    @Data
    public static class AiAsk {
        private String message;
        private Long campusId;
        private String date;
    }
}
