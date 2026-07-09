package com.seatwise.job;

import com.seatwise.entity.Reservation;
import com.seatwise.service.ReservationService;
import com.seatwise.service.WaitlistService;
import com.seatwise.sse.SseManager;
import com.seatwise.sse.UserSseManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务：
 * - 超时未签到自动释放（PENDING_SIGN_IN 超过签到窗口）
 * - 使用中自动完成（IN_USE 超过预约结束时间）
 * - SSE 心跳
 * 文档中延迟队列为主、扫描为兜底；演示场景用短周期扫描更直观可控。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledJobs {

    private final ReservationService reservationService;
    private final WaitlistService waitlistService;
    private final SseManager sse;
    private final UserSseManager userSse;

    @Scheduled(fixedDelay = 5000)
    public void releaseAndComplete() {
        LocalDateTime now = LocalDateTime.now();
        try {
            List<Reservation> pending = reservationService.findByStatus("PENDING_SIGN_IN");
            for (Reservation r : pending) {
                if (now.isAfter(reservationService.signinDeadline(r))) {
                    reservationService.releaseTimeout(r);
                    log.info("超时释放预约 {}", r.getId());
                }
            }
            List<Reservation> inUse = reservationService.findByStatus("IN_USE");
            for (Reservation r : inUse) {
                if (!now.isBefore(reservationService.reservationEnd(r))) {
                    reservationService.autoComplete(r);
                    log.info("自动完成预约 {}", r.getId());
                }
            }
            waitlistService.expireOffers();
        } catch (Exception e) {
            log.error("定时释放任务异常", e);
        }
    }

    @Scheduled(fixedDelay = 15000)
    public void heartbeat() {
        sse.heartbeatAll();
        userSse.heartbeatAll();
    }
}
