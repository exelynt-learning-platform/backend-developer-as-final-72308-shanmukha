package com.assignment.booking.config;

import com.assignment.booking.entity.Resource;
import com.assignment.booking.entity.Role;
import com.assignment.booking.entity.User;
import com.assignment.booking.enums.RoleName;
import com.assignment.booking.repository.ResourceRepository;
import com.assignment.booking.repository.RoleRepository;
import com.assignment.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Role[] roles = seedRoles();
        seedUsers(roles[0], roles[1]);
        seedResources();
    }

    private Role[] seedRoles() {
        Role userRole = seedRole(RoleName.ROLE_USER, "Standard user role");
        Role adminRole = seedRole(RoleName.ROLE_ADMIN, "Administrator role");
        return new Role[]{userRole, adminRole};
    }

    private Role seedRole(RoleName roleName, String description) {
        return roleRepository.findByName(roleName).orElseGet(() -> {
            Role role = roleRepository.save(Role.builder()
                    .name(roleName)
                    .description(description)
                    .build());
            log.info("Created {}", roleName);
            return role;
        });
    }

    private void seedUsers(Role userRole, Role adminRole) {

        if (!userRepository.existsByUsername("user")) {
            Set<Role> userRoles = new HashSet<>();
            userRoles.add(userRole);

            userRepository.save(User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("User@123"))
                    .email("user@booking.com")
                    .fullName("Regular User")
                    .enabled(true)
                    .roles(userRoles)
                    .build());
            log.info("Created default user: user");
        }

        if (!userRepository.existsByUsername("admin")) {
            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(adminRole);
            adminRoles.add(userRole);

            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("Admin@123"))
                    .email("admin@booking.com")
                    .fullName("Administrator")
                    .enabled(true)
                    .roles(adminRoles)
                    .build());
            log.info("Created default admin: admin");
        }
    }

    private void seedResources() {
        if (resourceRepository.count() == 0) {
            resourceRepository.save(Resource.builder()
                    .name("Conference Room A")
                    .description("Large conference room with projector and whiteboard")
                    .type("ROOM")
                    .available(true)
                    .pricePerUnit(BigDecimal.valueOf(50.00))
                    .location("Building A, Floor 2")
                    .capacity("20 people")
                    .build());

            resourceRepository.save(Resource.builder()
                    .name("Meeting Room B")
                    .description("Small meeting room for 4-6 people")
                    .type("ROOM")
                    .available(true)
                    .pricePerUnit(BigDecimal.valueOf(25.00))
                    .location("Building A, Floor 1")
                    .capacity("6 people")
                    .build());

            resourceRepository.save(Resource.builder()
                    .name("Company Car - Sedan")
                    .description("Toyota Camry 2024 for business travel")
                    .type("VEHICLE")
                    .available(true)
                    .pricePerUnit(BigDecimal.valueOf(75.00))
                    .location("Parking Garage")
                    .capacity("5 passengers")
                    .build());

            resourceRepository.save(Resource.builder()
                    .name("Projector")
                    .description("HD Projector with HDMI and wireless connectivity")
                    .type("EQUIPMENT")
                    .available(true)
                    .pricePerUnit(BigDecimal.valueOf(15.00))
                    .location("Equipment Room")
                    .capacity("N/A")
                    .build());

            resourceRepository.save(Resource.builder()
                    .name("Training Room")
                    .description("Large training room with multiple workstations")
                    .type("ROOM")
                    .available(true)
                    .pricePerUnit(BigDecimal.valueOf(100.00))
                    .location("Building B, Floor 3")
                    .capacity("30 people")
                    .build());

            log.info("Created sample resources");
        }
    }
}
