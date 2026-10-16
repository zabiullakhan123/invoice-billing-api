package com.example.billing.config;

import com.example.billing.entity.Role;
import com.example.billing.entity.User;
import com.example.billing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin account on startup if it doesn't exist yet. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername(adminUsername)) {
            return;
        }
        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.warn("Created initial admin user '{}'. Change the password before going to production.", adminUsername);
    }
}
