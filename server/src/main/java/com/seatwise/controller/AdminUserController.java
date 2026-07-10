package com.seatwise.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatwise.common.BizError;
import com.seatwise.common.BizException;
import com.seatwise.common.R;
import com.seatwise.entity.User;
import com.seatwise.mapper.UserMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * R6：管理员管理（仅主管理员 SUPER 可访问）。主管理员可创建/删除子管理员（ADMIN_SUB）。
 */
@RestController
@RequestMapping("/api/admin/admins")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @SaCheckRole("SUPER")
    @GetMapping
    public R<List<Map<String, Object>>> list() {
        List<User> admins = userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(User::getRole, List.of("ADMIN", "ADMIN_SUB")).orderByAsc(User::getId));
        return R.ok(admins.stream().map(u -> Map.<String, Object>of(
                "id", u.getId(), "username", u.getUsername(), "realName", u.getRealName(),
                "role", u.getRole(), "primary", "ADMIN".equals(u.getRole())
        )).toList());
    }

    @SaCheckRole("SUPER")
    @PostMapping
    public R<Map<String, Object>> create(@RequestBody AdminReq req) {
        if (req.getUsername() == null || req.getUsername().isBlank()
                || req.getPassword() == null || req.getPassword().length() < 6
                || req.getRealName() == null || req.getRealName().isBlank())
            throw new BizException(BizError.BAD_REQUEST, "用户名/姓名必填，密码至少 6 位");
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (exists != null && exists > 0) throw new BizException(BizError.USERNAME_EXISTS);
        User u = new User();
        u.setUsername(req.getUsername().trim());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setRealName(req.getRealName().trim());
        u.setRole("ADMIN_SUB");
        u.setCreditScore(0);
        u.setNoShowCount(0);
        userMapper.insert(u);
        return R.ok(Map.of("id", u.getId(), "username", u.getUsername()));
    }

    @SaCheckRole("SUPER")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        User u = userMapper.selectById(id);
        if (u == null) throw new BizException(BizError.BAD_REQUEST, "账号不存在");
        if ("ADMIN".equals(u.getRole())) throw new BizException(BizError.PERMISSION_DENIED, "不能删除主管理员");
        if (!"ADMIN_SUB".equals(u.getRole())) throw new BizException(BizError.PERMISSION_DENIED, "只能删除子管理员");
        Long self = Long.valueOf(StpUtil.getLoginId().toString());
        if (u.getId().equals(self)) throw new BizException(BizError.PERMISSION_DENIED, "不能删除自己");
        userMapper.deleteById(id);
        return R.ok();
    }

    @Data
    public static class AdminReq {
        private String username;
        private String password;
        private String realName;
    }
}
