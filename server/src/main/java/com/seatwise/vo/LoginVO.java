package com.seatwise.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private String role;
    private UserInfo userInfo;

    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String realName;
        private String role;
        private Integer creditScore;
        private Integer noShowCount;
    }
}
