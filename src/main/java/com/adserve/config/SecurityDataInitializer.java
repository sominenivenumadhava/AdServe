package com.adserve.config;

import com.adserve.entity.Advertiser;
import com.adserve.entity.Role;
import com.adserve.entity.User;
import com.adserve.repository.AdvertiserRepository;
import com.adserve.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes default ADMIN and ADVERTISER accounts on startup if not already seeded.
 */
@Component
@RequiredArgsConstructor
public class SecurityDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SecurityDataInitializer.class);

    private final UserRepository userRepository;
    private final AdvertiserRepository advertiserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 1. Initialize default Platform Administrator
        if (!userRepository.existsByEmail("admin@adserve.com")) {
            User admin = User.builder()
                    .name("AdServe Administrator")
                    .email("admin@adserve.com")
                    .password(passwordEncoder.encode("AdminPassword123!"))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("Initialized default ADMIN user: admin@adserve.com");
        }

        // 2. Initialize default Demo Advertiser
        if (!userRepository.existsByEmail("demo@techcorp.com")) {
            User advertiserUser = User.builder()
                    .name("TechCorp Solutions")
                    .email("demo@techcorp.com")
                    .password(passwordEncoder.encode("AdvertiserPassword123!"))
                    .role(Role.ADVERTISER)
                    .enabled(true)
                    .build();
            User savedUser = userRepository.save(advertiserUser);

            // Link to existing TechCorp advertiser or create new
            Advertiser advertiser = advertiserRepository.findByEmail("demo@techcorp.com")
                    .orElseGet(() -> Advertiser.builder()
                            .name("TechCorp Solutions")
                            .email("demo@techcorp.com")
                            .build());

            advertiser.setUser(savedUser);
            advertiserRepository.save(advertiser);
            log.info("Initialized default ADVERTISER user: demo@techcorp.com");
        }
    }
}
