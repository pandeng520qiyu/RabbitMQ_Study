package com.study.auth.controller;

import com.study.auth.dto.LoginRequest;
import com.study.auth.dto.LoginResponse;
import com.study.auth.dto.RegisterRequest;
import com.study.auth.entity.User;
import com.study.auth.service.AuthService;
import com.study.auth.util.JwtUtil;
import com.study.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 * <p>
 * 基础路径：/auth
 * 通过网关访问：http://localhost:8080/api/auth/...
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    /**
     * 用户注册
     * POST /auth/register
     *
     * 请求体示例：
     * {
     *   "username": "admin",
     *   "password": "123456",
     *   "email": "admin@example.com"
     * }
     */
    @PostMapping("/register")
    public ApiResponse<String> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ApiResponse.success("注册成功", "用户 [" + user.getUsername() + "] 注册成功");
    }

    /**
     * 用户登录
     * POST /auth/login
     *
     * 请求体示例：
     * {
     *   "username": "admin",
     *   "password": "123456"
     * }
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success("登录成功", response);
    }

    /**
     * 校验 Token 是否有效（供网关调用）
     * GET /auth/validate?token=xxx
     */
    @GetMapping("/validate")
    public ApiResponse<Boolean> validate(@RequestParam String token) {
        boolean valid = jwtUtil.validateToken(token);
        return ApiResponse.success(valid);
    }
}
