package com.adserve.controller;

import com.adserve.config.AppConfig;
import com.adserve.dto.AdvertiserRequestDto;
import com.adserve.dto.AdvertiserResponseDto;
import com.adserve.exception.DuplicateResourceException;
import com.adserve.exception.GlobalExceptionHandler;
import com.adserve.service.AdvertiserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdvertiserController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@Import({AppConfig.class, GlobalExceptionHandler.class})
class AdvertiserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdvertiserService advertiserService;

    @Test
    @DisplayName("POST /api/advertisers returns 201 when valid")
    void testCreateAdvertiserSuccess() throws Exception {
        AdvertiserRequestDto request = AdvertiserRequestDto.builder()
                .name("Acme Corp")
                .email("contact@acme.com")
                .build();

        AdvertiserResponseDto response = AdvertiserResponseDto.builder()
                .id(1L)
                .name("Acme Corp")
                .email("contact@acme.com")
                .createdAt(LocalDateTime.now())
                .campaignCount(0)
                .build();

        when(advertiserService.createAdvertiser(any(AdvertiserRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/advertisers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.name", is("Acme Corp")))
                .andExpect(jsonPath("$.data.email", is("contact@acme.com")));
    }

    @Test
    @DisplayName("POST /api/advertisers returns 400 when name is blank or email is invalid")
    void testCreateAdvertiserInvalidInputReturns400() throws Exception {
        AdvertiserRequestDto invalidRequest = AdvertiserRequestDto.builder()
                .name("")
                .email("invalid-email-string")
                .build();

        mockMvc.perform(post("/api/advertisers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("POST /api/advertisers returns 409 Conflict when email already exists")
    void testCreateAdvertiserDuplicateEmailReturns409() throws Exception {
        AdvertiserRequestDto request = AdvertiserRequestDto.builder()
                .name("Acme Corp")
                .email("duplicate@acme.com")
                .build();

        when(advertiserService.createAdvertiser(any(AdvertiserRequestDto.class)))
                .thenThrow(new DuplicateResourceException("Advertiser", "email", "duplicate@acme.com"));

        mockMvc.perform(post("/api/advertisers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")));
    }

    @Test
    @DisplayName("GET /api/advertisers returns 200 OK")
    void testGetAllAdvertisers() throws Exception {
        AdvertiserResponseDto response = AdvertiserResponseDto.builder()
                .id(1L)
                .name("Acme Corp")
                .email("contact@acme.com")
                .createdAt(LocalDateTime.now())
                .campaignCount(2)
                .build();

        when(advertiserService.getAllAdvertisers()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/advertisers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].id", is(1)));
    }
}
