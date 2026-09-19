package com.adserve.controller;

import com.adserve.config.AppConfig;
import com.adserve.dto.CampaignRequestDto;
import com.adserve.dto.CampaignResponseDto;
import com.adserve.entity.CampaignStatus;
import com.adserve.exception.GlobalExceptionHandler;
import com.adserve.service.CampaignService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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

@WebMvcTest(CampaignController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@Import({AppConfig.class, GlobalExceptionHandler.class})
class CampaignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CampaignService campaignService;

    @Test
    @DisplayName("POST /api/campaigns returns 201 when valid")
    void testCreateCampaignSuccess() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        CampaignRequestDto request = CampaignRequestDto.builder()
                .advertiserId(1L)
                .name("Black Friday 2026")
                .budget(BigDecimal.valueOf(10000.00))
                .startDate(now)
                .endDate(now.plusDays(10))
                .status(CampaignStatus.ACTIVE)
                .targetCountry("US")
                .targetDevice("DESKTOP")
                .targetCategory("TECH")
                .build();

        CampaignResponseDto response = CampaignResponseDto.builder()
                .id(20L)
                .advertiserId(1L)
                .advertiserName("Acme Corp")
                .name("Black Friday 2026")
                .budget(BigDecimal.valueOf(10000.00))
                .startDate(now)
                .endDate(now.plusDays(10))
                .status(CampaignStatus.ACTIVE)
                .targetCountry("US")
                .targetDevice("DESKTOP")
                .targetCategory("TECH")
                .createdAt(now)
                .adCount(0)
                .build();

        when(campaignService.createCampaign(any(CampaignRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(20)))
                .andExpect(jsonPath("$.data.name", is("Black Friday 2026")));
    }

    @Test
    @DisplayName("GET /api/campaigns returns 200 OK")
    void testGetAllCampaigns() throws Exception {
        CampaignResponseDto response = CampaignResponseDto.builder()
                .id(20L)
                .advertiserId(1L)
                .advertiserName("Acme Corp")
                .name("Black Friday 2026")
                .budget(BigDecimal.valueOf(10000.00))
                .status(CampaignStatus.ACTIVE)
                .build();

        when(campaignService.getAllCampaigns()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].id", is(20)));
    }

    @Test
    @DisplayName("DELETE /api/campaigns/{id} returns 200 OK")
    void testDeleteCampaign() throws Exception {
        doNothing().when(campaignService).deleteCampaign(eq(20L));

        mockMvc.perform(delete("/api/campaigns/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Campaign deleted successfully")));
    }
}
