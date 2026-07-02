package com.study.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应 DTO
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    // JWT 令牌
    private String token;

    // 令牌类型，固定为 Bearer
    private String tokenType;

    // 过期时间（秒）
    private long expiresIn;

    // 当前用户信息
    private UserInfo userInfo;

    @Data
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String role;
    }
}
