package com.seatwise.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.seatwise.common.R;
import com.seatwise.common.SlotUtil;
import com.seatwise.config.SeatwiseProps;
import com.seatwise.service.BoardService;
import com.seatwise.sse.SseManager;
import com.seatwise.vo.BoardVO;
import com.seatwise.vo.ReplayVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final SseManager sseManager;
    private final SeatwiseProps props;

    @GetMapping("/study-rooms/{id}/board")
    public R<BoardVO> board(@PathVariable Long id,
                            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime start,
                            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime end) {
        int slotMin = props.getSlotMinutes();
        int startSlot = start != null ? SlotUtil.toSlot(start, slotMin) : 0;
        int endSlot = end != null ? SlotUtil.toSlot(end, slotMin) : 48;
        Long uid = currentUserOrNull();
        return R.ok(boardService.buildBoard(id, date, startSlot, endSlot, uid));
    }

    /** 历史回放：按时间片重建当天座位占用轨迹 */
    @GetMapping("/study-rooms/{id}/replay")
    public R<ReplayVO> replay(@PathVariable Long id,
                              @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return R.ok(boardService.buildReplay(id, date));
    }

    /**
     * SSE 订阅。token 通过查询参数传递（EventSource 不能自定义请求头）。
     */
    @GetMapping(value = "/board/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter stream(@RequestParam Long roomId,
                             @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                             @RequestParam(required = false) String token) {
        return sseManager.subscribe(roomId, date);
    }

    private Long currentUserOrNull() {
        try {
            if (StpUtil.isLogin()) {
                return Long.valueOf(StpUtil.getLoginId().toString());
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
