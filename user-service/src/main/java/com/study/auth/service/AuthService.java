package com.study.auth.service;

import com.study.auth.dto.LoginRequest;
import com.study.auth.dto.LoginResponse;
import com.study.auth.dto.RegisterRequest;
import com.study.auth.entity.User;
import com.study.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;

/**
 * 认证服务
 * <p>
 * 处理用户注册、登录逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 用户注册
     */
    @Transactional
    public User register(RegisterRequest request) {
        // 检查用户名是否已存在
        List<User> existing = entityManager
                .createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", request.getUsername())
                .getResultList();

        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("用户名 [" + request.getUsername() + "] 已存在");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role("USER")
                .build();

        entityManager.persist(user);
        log.info("新用户注册成功: {}", user.getUsername());
        return user;
    }

    /**
     * 用户登录，返回 JWT
     */
    public LoginResponse login(LoginRequest request) {
        TypedQuery<User> query = entityManager
                .createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", request.getUsername());

        List<User> users = query.getResultList();
        if (users.isEmpty()) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        User user = users.get(0);
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        log.info("用户登录成功: {}", user.getUsername());

        return new LoginResponse(
                token,
                "Bearer",
                jwtUtil.getExpiration(),
                new LoginResponse.UserInfo(user.getId(), user.getUsername(), user.getRole())
        );
    }
}
