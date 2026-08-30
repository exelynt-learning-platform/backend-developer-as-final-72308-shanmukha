package com.assignment.booking.config;

import com.assignment.booking.entity.Resource;
import com.assignment.booking.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResourceSeeder {

    private final ResourceRepository resourceRepository;

    @Transactional
    public void seed() {
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
