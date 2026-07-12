package com.seatwise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordDTO {
    @NotBlank(message = "请输入用户名")
    private String username;
    @NotBlank(message = "请输入姓名")
    private String realName;
    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 64, message = "密码至少 6 位")
    private String newPassword;
    private String captchaId;
    private String captchaCode;
}
