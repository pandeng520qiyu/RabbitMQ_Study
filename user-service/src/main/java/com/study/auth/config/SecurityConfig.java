package com.study.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置
 * <p>
 * 用户服务本身不做 JWT 校验，只负责颁发 Token；
 * 所有接口均放开，由网关统一鉴权。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 关闭 CSRF：本服务为无状态 JWT REST API
                // JWT 存储在 localStorage 并通过 Authorization 请求头发送，
                // 浏览器不会自动携带该 Header 进行跨站请求，因此不存在 CSRF 风险。
                // 参考：https://security.stackexchange.com/a/166798
                .csrf(AbstractHttpConfigurer::disable)
                // 无状态 Session（JWT 无需 Session）
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 放开所有接口（鉴权由网关统一负责）
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
