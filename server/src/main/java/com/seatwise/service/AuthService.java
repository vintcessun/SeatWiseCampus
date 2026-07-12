package com.seatwise.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatwise.common.BizError;
import com.seatwise.common.BizException;
import com.seatwise.dto.LoginDTO;
import com.seatwise.dto.RegisterDTO;
import com.seatwise.dto.ResetPasswordDTO;
import com.seatwise.entity.User;
import com.seatwise.mapper.UserMapper;
import com.seatwise.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.BeanUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;

    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(BizError.LOGIN_FAILED);
        }
        StpUtil.login(user.getId());
        StpUtil.getSession().set("role", user.getRole());

        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setRole(user.getRole());
        vo.setUserInfo(toInfo(user));
        return vo;
    }

    public LoginVO register(RegisterDTO dto) {
        if (!captchaService.validate(dto.getCaptchaId(), dto.getCaptchaCode())) {
            throw new BizException(BizError.CAPTCHA_INVALID);
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BizException(BizError.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setRole("STUDENT");
        user.setCreditScore(0);
        user.setNoShowCount(0);
        userMapper.insert(user);

        LoginDTO login = new LoginDTO();
        login.setUsername(dto.getUsername());
        login.setPassword(dto.getPassword());
        return login(login);
    }

    public void resetPassword(ResetPasswordDTO dto) {
        if (!captchaService.validate(dto.getCaptchaId(), dto.getCaptchaCode())) {
            throw new BizException(BizError.CAPTCHA_INVALID);
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername())
                .eq(User::getRealName, dto.getRealName()));
        if (user == null) {
            throw new BizException(BizError.LOGIN_FAILED);
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    public LoginVO.UserInfo me() {
        Long uid = currentUserId();
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new BizException(BizError.AUTH_REQUIRED);
        }
        return toInfo(user);
    }

    public Long currentUserId() {
        return Long.valueOf(StpUtil.getLoginId().toString());
    }

    private LoginVO.UserInfo toInfo(User user) {
        LoginVO.UserInfo info = new LoginVO.UserInfo();
        BeanUtils.copyProperties(user, info);
        return info;
    }
}
