package com.adserve.security;

import com.adserve.config.AppConfig;
import com.adserve.config.SecurityConfig;
import com.adserve.controller.AdServerController;
import com.adserve.controller.CampaignController;
import com.adserve.dto.AdResponseDto;
import com.adserve.dto.CampaignResponseDto;
import com.adserve.entity.CampaignStatus;
import com.adserve.entity.Role;
import com.adserve.exception.ForbiddenException;
import com.adserve.exception.GlobalExceptionHandler;
import com.adserve.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CampaignController.class, AdServerController.class})
@Import({AppConfig.class, SecurityConfig.class, GlobalExceptionHandler.class})
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CampaignService campaignService;

    @MockBean
    private AdServingService adServingService;

    @MockBean
    private ImpressionService impressionService;

    @MockBean
    private ClickService clickService;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private CacheMetricsService cacheMetricsService;

    @MockBean
    private AdCacheService adCacheService;

    @MockBean
    private AdEventProducer adEventProducer;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Public API: GET /api/ad-server/serve is accessible without authentication")
    void testPublicAdServingWithoutToken() throws Exception {
        AdResponseDto responseDto = AdResponseDto.builder()
                .adId(101L)
                .campaignId(20L)
                .title("Public Gaming Ad")
                .imageUrl("https://example.com/ad.jpg")
                .targetUrl("https://example.com/buy")
                .build();

        when(adServingService.serveAd(any())).thenReturn(responseDto);

        mockMvc.perform(get("/api/ad-server/serve")
                        .param("country", "IN")
                        .param("device", "ANDROID")
                        .param("category", "GAMING")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.adId", is(101)));
    }

    @Test
    @DisplayName("Public API: POST /api/ad-server/101/impression is accessible without authentication")
    void testPublicImpressionWithoutToken() throws Exception {
        mockMvc.perform(post("/api/ad-server/101/impression")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("Protected API: GET /api/campaigns without token returns 401 Unauthorized")
    void testProtectedCampaignsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/campaigns")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @WithMockUser(username = "advertiser_b@example.com", roles = {"ADVERTISER"})
    @DisplayName("Ownership check: Advertiser accessing another advertiser's campaign returns 403 Forbidden")
    void testAdvertiserAccessingAnotherCampaignForbidden() throws Exception {
        when(campaignService.getCampaignById(eq(101L)))
                .thenThrow(new ForbiddenException("Campaign", 101L));

        mockMvc.perform(get("/api/campaigns/101")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access denied: You do not have permission to access Campaign with id '101'")));
    }

    @Test
    @WithMockUser(username = "advertiser_a@example.com", roles = {"ADVERTISER"})
    @DisplayName("Ownership check: Advertiser accessing own campaign returns 200 OK")
    void testAdvertiserAccessingOwnCampaignSuccess() throws Exception {
        CampaignResponseDto campaign = CampaignResponseDto.builder()
                .id(101L)
                .advertiserId(5L)
                .name("Summer Blitz")
                .budget(BigDecimal.valueOf(5000.00))
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .status(CampaignStatus.ACTIVE)
                .targetCountry("IN")
                .targetDevice("ANDROID")
                .targetCategory("GAMING")
                .build();

        when(campaignService.getCampaignById(eq(101L))).thenReturn(campaign);

        mockMvc.perform(get("/api/campaigns/101")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(101)))
                .andExpect(jsonPath("$.data.name", is("Summer Blitz")));
    }
}
