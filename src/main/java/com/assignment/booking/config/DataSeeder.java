package com.assignment.booking.config;

import com.assignment.booking.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
        Role[] roles = roleSeeder.seed();
        userSeeder.seed(roles[0], roles[1]);
        resourceSeeder.seed();
        log.info("Dev data seeding complete.");
    }
}
