package com.adserve.controller;

import com.adserve.config.AppConfig;
import com.adserve.config.SecurityConfig;
import com.adserve.dto.*;
import com.adserve.exception.DuplicateResourceException;
import com.adserve.exception.GlobalExceptionHandler;
import com.adserve.security.CustomUserDetailsService;
import com.adserve.security.JwtService;
import com.adserve.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({AppConfig.class, SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/auth/register successfully registers an advertiser and returns 201 with JWT")
    void testRegisterSuccess() throws Exception {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .name("Alice Advertiser")
                .email("alice@example.com")
                .password("StrongPassword123")
                .build();

        AuthResponseDto responseDto = AuthResponseDto.builder()
                .token("mocked.jwt.token")
                .tokenType("Bearer")
                .expiresIn(3600)
                .user(UserSummaryDto.builder()
                        .id(1L)
                        .name("Alice Advertiser")
                        .email("alice@example.com")
                        .role("ADVERTISER")
                        .advertiserId(10L)
                        .build())
                .build();

        when(authService.register(any(RegisterRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", is("mocked.jwt.token")))
                .andExpect(jsonPath("$.data.user.role", is("ADVERTISER")))
                .andExpect(jsonPath("$.data.user.email", is("alice@example.com")));
    }

    @Test
    @DisplayName("POST /api/auth/register with duplicate email returns 409 Conflict")
    void testRegisterDuplicateEmail() throws Exception {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .name("Alice Duplicate")
                .email("alice@example.com")
                .password("StrongPassword123")
                .build();

        when(authService.register(any(RegisterRequestDto.class)))
                .thenThrow(new DuplicateResourceException("User", "email", "alice@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", is("User already exists with email: 'alice@example.com'")));
    }

    @Test
    @DisplayName("POST /api/auth/register with invalid email returns 400 Bad Request")
    void testRegisterInvalidEmail() throws Exception {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .name("Invalid Email")
                .email("not-an-email")
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("POST /api/auth/login with valid credentials returns 200 with JWT")
    void testLoginSuccess() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("alice@example.com")
                .password("StrongPassword123")
                .build();

        AuthResponseDto responseDto = AuthResponseDto.builder()
                .token("valid.jwt.token")
                .tokenType("Bearer")
                .expiresIn(3600)
                .user(UserSummaryDto.builder()
                        .id(1L)
                        .name("Alice Advertiser")
                        .email("alice@example.com")
                        .role("ADVERTISER")
                        .advertiserId(10L)
                        .build())
                .build();

        when(authService.login(any(LoginRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", is("valid.jwt.token")))
                .andExpect(jsonPath("$.data.user.email", is("alice@example.com")));
    }

    @Test
    @DisplayName("POST /api/auth/login with wrong credentials returns 401 Unauthorized")
    void testLoginBadCredentials() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("alice@example.com")
                .password("WrongPassword")
                .build();

        when(authService.login(any(LoginRequestDto.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)));
    }
}
