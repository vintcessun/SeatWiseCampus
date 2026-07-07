package com.seatwise.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatwise.common.R;
import com.seatwise.entity.BlacklistRecord;
import com.seatwise.mapper.BlacklistMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BlacklistController {

    private final BlacklistMapper blacklistMapper;

    @SaCheckLogin
    @GetMapping("/blacklist/me")
    public R<BlacklistRecord> me() {
        Long uid = Long.valueOf(StpUtil.getLoginId().toString());
        BlacklistRecord rec = blacklistMapper.selectOne(new LambdaQueryWrapper<BlacklistRecord>()
                .eq(BlacklistRecord::getUserId, uid).eq(BlacklistRecord::getActive, 1)
                .gt(BlacklistRecord::getEndTime, LocalDateTime.now()).last("limit 1"));
        return R.ok(rec);
    }

    @SaCheckRole("ADMIN")
    @GetMapping("/admin/blacklist")
    public R<List<BlacklistRecord>> list() {
        return R.ok(blacklistMapper.selectList(new LambdaQueryWrapper<BlacklistRecord>()
                .orderByDesc(BlacklistRecord::getCreatedTime)));
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/admin/blacklist/{id}/release")
    public R<Void> release(@PathVariable Long id) {
        BlacklistRecord rec = blacklistMapper.selectById(id);
        if (rec != null) {
            rec.setActive(0);
            blacklistMapper.updateById(rec);
        }
        return R.ok();
    }
}
