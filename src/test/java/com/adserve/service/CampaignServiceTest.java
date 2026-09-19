package com.adserve.service;

import com.adserve.dto.CampaignRequestDto;
import com.adserve.dto.CampaignResponseDto;
import com.adserve.entity.Advertiser;
import com.adserve.entity.Campaign;
import com.adserve.entity.CampaignStatus;
import com.adserve.exception.BadRequestException;
import com.adserve.exception.ResourceNotFoundException;
import com.adserve.repository.CampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private AdvertiserService advertiserService;

    @Mock
    private AdCacheService adCacheService;

    @InjectMocks
    private CampaignService campaignService;

    private Advertiser advertiser;
    private Campaign campaign;
    private CampaignRequestDto requestDto;

    @BeforeEach
    void setUp() {
        advertiser = Advertiser.builder()
                .id(1L)
                .name("Acme Corp")
                .email("contact@acme.com")
                .build();

        LocalDateTime now = LocalDateTime.now();

        campaign = Campaign.builder()
                .id(10L)
                .advertiser(advertiser)
                .name("Summer Sale 2026")
                .budget(BigDecimal.valueOf(5000.00))
                .startDate(now)
                .endDate(now.plusDays(30))
                .status(CampaignStatus.ACTIVE)
                .targetCountry("IN")
                .targetDevice("ANDROID")
                .targetCategory("GAMING")
                .createdAt(now)
                .advertisements(Collections.emptyList())
                .build();

        requestDto = CampaignRequestDto.builder()
                .advertiserId(1L)
                .name("Summer Sale 2026")
                .budget(BigDecimal.valueOf(5000.00))
                .startDate(now)
                .endDate(now.plusDays(30))
                .status(CampaignStatus.ACTIVE)
                .targetCountry("IN")
                .targetDevice("ANDROID")
                .targetCategory("GAMING")
                .build();
    }

    @Test
    @DisplayName("Successfully creates a campaign when all validations pass")
    void testCreateCampaignSuccess() {
        when(advertiserService.getAdvertiserEntity(1L)).thenReturn(advertiser);
        when(campaignRepository.save(any(Campaign.class))).thenReturn(campaign);

        CampaignResponseDto response = campaignService.createCampaign(requestDto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getAdvertiserId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Summer Sale 2026");
        assertThat(response.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
    }

    @Test
    @DisplayName("Throws BadRequestException when budget is zero or negative")
    void testCreateCampaignZeroBudgetThrows() {
        requestDto.setBudget(BigDecimal.ZERO);

        assertThatThrownBy(() -> campaignService.createCampaign(requestDto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("budget must be greater than zero");

        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    @Test
    @DisplayName("Throws BadRequestException when end date is before start date")
    void testCreateCampaignInvalidDateRangeThrows() {
        requestDto.setStartDate(LocalDateTime.now().plusDays(10));
        requestDto.setEndDate(LocalDateTime.now().plusDays(5));

        assertThatThrownBy(() -> campaignService.createCampaign(requestDto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("end date cannot be before start date");

        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when referenced advertiser does not exist")
    void testCreateCampaignAdvertiserNotFoundThrows() {
        when(advertiserService.getAdvertiserEntity(1L))
                .thenThrow(new ResourceNotFoundException("Advertiser", "id", 1L));

        assertThatThrownBy(() -> campaignService.createCampaign(requestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Advertiser not found");

        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    @Test
    @DisplayName("Deletes campaign when ID exists")
    void testDeleteCampaignSuccess() {
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        doNothing().when(campaignRepository).delete(campaign);

        campaignService.deleteCampaign(10L);

        verify(campaignRepository).delete(campaign);
    }
}
