package com.seatwise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank(message = "请输入用户名")
    @Size(min = 3, max = 32, message = "用户名 3-32 位")
    private String username;
    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 64, message = "密码至少 6 位")
    private String password;
    @NotBlank(message = "请输入姓名")
    private String realName;
}
