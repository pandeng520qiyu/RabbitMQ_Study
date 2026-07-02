package com.study.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 全局鉴权过滤器
 * <p>
 * 规则：
 * 1. /api/auth/login、/api/auth/register、/api/auth/validate 等白名单路径无需 Token
 * 2. 其余路径必须携带有效 ****** 401
 * 3. 解析成功后将 userId、username、role 注入下游请求 Header
 */
@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    // 与 user-service 保持相同的默认密钥
    @Value("${jwt.secret:rabbitmq-study-secret-key-must-be-at-least-32bytes!}")
    private String secret;

    // 不需要鉴权的路径前缀
    private static final List<String> WHITE_LIST = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/validate",
            "/actuator"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        // 白名单放行
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "缺少 Authorization 请求头，请先登录");
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 将用户信息注入下游请求 Header
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", String.valueOf(claims.get("userId")))
                    .header("X-Username", claims.getSubject())
                    .header("X-User-Role", String.valueOf(claims.get("role")))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JwtException e) {
            log.warn("JWT 校验失败: path={}, error={}", path, e.getMessage());
            return unauthorized(exchange, "Token 无效或已过期，请重新登录");
        }
    }

    @Override
    public int getOrder() {
        return -100; // 优先级最高
    }

    private boolean isWhitelisted(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
