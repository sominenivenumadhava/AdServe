package com.adserve.service;

import com.adserve.dto.AuthResponseDto;
import com.adserve.dto.LoginRequestDto;
import com.adserve.dto.RegisterRequestDto;
import com.adserve.dto.UserSummaryDto;
import com.adserve.entity.Advertiser;
import com.adserve.entity.Role;
import com.adserve.entity.User;
import com.adserve.exception.DuplicateResourceException;
import com.adserve.repository.AdvertiserRepository;
import com.adserve.repository.UserRepository;
import com.adserve.security.JwtService;
import com.adserve.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user authentication, registration, and JWT token issuance.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final AdvertiserRepository advertiserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new user with default ADVERTISER role.
     * Automatically creates or associates an Advertiser profile.
     */
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User", "email", email);
        }

        // 1. Create and persist User entity
        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ADVERTISER)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        // 2. Link or create associated Advertiser profile
        Advertiser advertiser = advertiserRepository.findByEmail(email)
                .orElseGet(() -> Advertiser.builder()
                        .name(savedUser.getName())
                        .email(savedUser.getEmail())
                        .build());

        advertiser.setUser(savedUser);
        Advertiser savedAdvertiser = advertiserRepository.save(advertiser);

        log.info("Registered new advertiser user [id={}, email={}] linked to advertiserId: {}",
                savedUser.getId(), savedUser.getEmail(), savedAdvertiser.getId());

        // 3. Issue JWT Token
        UserPrincipal principal = UserPrincipal.create(savedUser, savedAdvertiser.getId());
        String token = jwtService.generateToken(principal);

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs() / 1000)
                .user(UserSummaryDto.builder()
                        .id(savedUser.getId())
                        .name(savedUser.getName())
                        .email(savedUser.getEmail())
                        .role(savedUser.getRole().name())
                        .advertiserId(savedAdvertiser.getId())
                        .build())
                .build();
    }

    /**
     * Authenticates user credentials via Spring Security AuthenticationManager and returns JWT.
     */
    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto request) {
        String email = request.getEmail().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        String token = jwtService.generateToken(principal);

        log.info("Successful login for user [id={}, email={}, role={}]",
                principal.getId(), principal.getEmail(), principal.getRole());

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs() / 1000)
                .user(UserSummaryDto.builder()
                        .id(principal.getId())
                        .name(principal.getName())
                        .email(principal.getEmail())
                        .role(principal.getRole().name())
                        .advertiserId(principal.getAdvertiserId())
                        .build())
                .build();
    }

    /**
     * Retrieves current user summary from authenticated principal.
     */
    public UserSummaryDto getCurrentUser(UserPrincipal principal) {
        return UserSummaryDto.builder()
                .id(principal.getId())
                .name(principal.getName())
                .email(principal.getEmail())
                .role(principal.getRole().name())
                .advertiserId(principal.getAdvertiserId())
                .build();
    }
}
