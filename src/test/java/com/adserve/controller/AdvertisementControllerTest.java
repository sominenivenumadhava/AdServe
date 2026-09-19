package com.adserve.controller;

import com.adserve.config.AppConfig;
import com.adserve.dto.AdvertisementRequestDto;
import com.adserve.dto.AdvertisementResponseDto;
import com.adserve.entity.AdStatus;
import com.adserve.exception.GlobalExceptionHandler;
import com.adserve.service.AdvertisementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdvertisementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({AppConfig.class, GlobalExceptionHandler.class})
class AdvertisementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdvertisementService advertisementService;

    @Test
    @DisplayName("POST /api/ads returns 201 when valid")
    void testCreateAdvertisementSuccess() throws Exception {
        AdvertisementRequestDto request = AdvertisementRequestDto.builder()
                .campaignId(20L)
                .title("50% Off Ultra Laptops")
                .imageUrl("https://cdn.example.com/laptop.jpg")
                .targetUrl("https://example.com/buy")
                .status(AdStatus.ACTIVE)
                .build();

        AdvertisementResponseDto response = AdvertisementResponseDto.builder()
                .id(101L)
                .campaignId(20L)
                .campaignName("Black Friday 2026")
                .title("50% Off Ultra Laptops")
                .imageUrl("https://cdn.example.com/laptop.jpg")
                .targetUrl("https://example.com/buy")
                .status(AdStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(advertisementService.createAdvertisement(any(AdvertisementRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/ads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(101)))
                .andExpect(jsonPath("$.data.title", is("50% Off Ultra Laptops")));
    }

    @Test
    @DisplayName("GET /api/ads returns 200 OK")
    void testGetAllAdvertisements() throws Exception {
        AdvertisementResponseDto response = AdvertisementResponseDto.builder()
                .id(101L)
                .campaignId(20L)
                .campaignName("Black Friday 2026")
                .title("50% Off Ultra Laptops")
                .status(AdStatus.ACTIVE)
                .build();

        when(advertisementService.getAllAdvertisements()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/ads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].id", is(101)));
    }

    @Test
    @DisplayName("DELETE /api/ads/{id} returns 200 OK")
    void testDeleteAdvertisement() throws Exception {
        doNothing().when(advertisementService).deleteAdvertisement(eq(101L));

        mockMvc.perform(delete("/api/ads/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Advertisement deleted successfully")));
    }
}
