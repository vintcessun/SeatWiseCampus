package com.seatwise.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.seatwise.common.R;
import com.seatwise.dto.LoginDTO;
import com.seatwise.service.AuthService;
import com.seatwise.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @PostMapping("/auth/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    @SaCheckLogin
    @GetMapping("/users/me")
    public R<LoginVO.UserInfo> me() {
        return R.ok(authService.me());
    }
}
