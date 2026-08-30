package com.assignment.booking.config;

import com.assignment.booking.entity.Role;
import com.assignment.booking.enums.RoleName;
import com.assignment.booking.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleSeeder {

    private final RoleRepository roleRepository;

    @Transactional
    public Role[] seed() {
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
}
