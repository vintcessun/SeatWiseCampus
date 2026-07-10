package com.seatwise.config;

import cn.dev33.satoken.stp.StpInterface;
import com.seatwise.entity.User;
import com.seatwise.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 为 Sa-Token 提供角色列表，支撑 @SaCheckRole。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        User user = userMapper.selectById(Long.valueOf(loginId.toString()));
        if (user == null) {
            return Collections.emptyList();
        }
        String role = user.getRole();
        // 主管理员额外拥有 SUPER（可管理其他管理员）；子管理员拥有 ADMIN 以复用常规管理端能力
        if ("ADMIN".equals(role)) return List.of("ADMIN", "SUPER");
        if ("ADMIN_SUB".equals(role)) return List.of("ADMIN_SUB", "ADMIN");
        return List.of(role);
    }
}
