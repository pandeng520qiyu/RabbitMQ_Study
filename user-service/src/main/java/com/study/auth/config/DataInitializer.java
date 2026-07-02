package com.study.auth.config;

import com.study.auth.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 初始化默认数据
 * <p>
 * 应用启动时自动插入 admin / guest 两个演示账户
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initUser("admin", "admin123", "ADMIN");
        initUser("guest", "guest123", "USER");
    }

    private void initUser(String username, String password, String role) {
        long count = entityManager
                .createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult();

        if (count == 0) {
            User user = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .role(role)
                    .build();
            entityManager.persist(user);
            log.info("初始化用户: {} / {} ({})", username, password, role);
        }
    }
}
