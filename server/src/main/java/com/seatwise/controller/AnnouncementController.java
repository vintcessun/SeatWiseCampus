package com.seatwise.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.seatwise.common.R;
import com.seatwise.entity.Announcement;
import com.seatwise.service.AnnouncementService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService service;

    private Long uid() { return Long.valueOf(StpUtil.getLoginId().toString()); }

    /** 公开：生效中的公告（学生/管理员首页与列表用） */
    @SaCheckLogin
    @GetMapping("/api/announcements")
    public R<List<Announcement>> list() {
        return R.ok(service.listActive());
    }

    @SaCheckRole("ADMIN")
    @GetMapping("/api/admin/announcements")
    public R<List<Announcement>> listAll() {
        return R.ok(service.listAll());
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/api/admin/announcements")
    public R<Announcement> create(@RequestBody AnnReq req) {
        return R.ok(service.create(uid(), req.getTitle(), req.getContent(), req.getLevel(), req.isNotifyAll()));
    }

    @SaCheckRole("ADMIN")
    @PutMapping("/api/admin/announcements/{id}")
    public R<Announcement> update(@PathVariable Long id, @RequestBody AnnReq req) {
        return R.ok(service.update(id, req.getTitle(), req.getContent(), req.getLevel(), req.getActive()));
    }

    @SaCheckRole("ADMIN")
    @DeleteMapping("/api/admin/announcements/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @Data
    public static class AnnReq {
        private String title;
        private String content;
        private String level;       // INFO / WARN
        private Integer active;
        private boolean notifyAll;
    }
}
