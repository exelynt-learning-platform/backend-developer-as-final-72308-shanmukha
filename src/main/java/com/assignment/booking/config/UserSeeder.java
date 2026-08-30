package com.assignment.booking.config;

import com.assignment.booking.entity.Role;
import com.assignment.booking.entity.User;
import com.assignment.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${SEED_USER_PASSWORD:}")
    private String userPassword;

    @Value("${SEED_ADMIN_PASSWORD:}")
    private String adminPassword;

    @Transactional
    public void seed(Role userRole, Role adminRole) {
        if (userPassword == null || userPassword.isBlank() ||
            adminPassword == null || adminPassword.isBlank()) {
            log.warn("SEED_USER_PASSWORD and SEED_ADMIN_PASSWORD must be set — skipping user seed");
            return;
        }

        seedUser(userRole);
        seedAdmin(userRole, adminRole);
    }

    private void seedUser(Role userRole) {
        if (!userRepository.existsByUsername("user")) {
            Set<Role> roles = new HashSet<>();
            roles.add(userRole);

            userRepository.save(User.builder()
                    .username("user")
                    .password(passwordEncoder.encode(userPassword))
                    .email("user@booking.com")
                    .fullName("Regular User")
                    .enabled(true)
                    .roles(roles)
                    .build());
            log.info("Created default user: user");
        }
    }

    private void seedAdmin(Role userRole, Role adminRole) {
        if (!userRepository.existsByUsername("admin")) {
            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            roles.add(userRole);

            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode(adminPassword))
                    .email("admin@booking.com")
                    .fullName("Administrator")
                    .enabled(true)
                    .roles(roles)
                    .build());
            log.info("Created default admin: admin");
        }
    }
}
