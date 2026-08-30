package com.assignment.booking.config;

import com.assignment.booking.entity.Role;
import com.assignment.booking.enums.RoleName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final RoleSeeder roleSeeder;
    private final UserSeeder userSeeder;
    private final ResourceSeeder resourceSeeder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting dev data seeding...");
        Map<RoleName, Role> roles = roleSeeder.seed();
        userSeeder.seed(roles.get(RoleName.ROLE_USER), roles.get(RoleName.ROLE_ADMIN));
        resourceSeeder.seed();
        log.info("Dev data seeding complete.");
    }
}
