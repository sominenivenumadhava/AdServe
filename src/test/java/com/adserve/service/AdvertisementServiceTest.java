package com.adserve.service;

import com.adserve.dto.AdvertisementRequestDto;
import com.adserve.dto.AdvertisementResponseDto;
import com.adserve.entity.AdStatus;
import com.adserve.entity.Advertisement;
import com.adserve.entity.Campaign;
import com.adserve.exception.ResourceNotFoundException;
import com.adserve.repository.AdvertisementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvertisementServiceTest {

    @Mock
    private AdvertisementRepository advertisementRepository;

    @Mock
    private CampaignService campaignService;

    @Mock
    private AdCacheService adCacheService;

    @InjectMocks
    private AdvertisementService advertisementService;

    private Campaign campaign;
    private Advertisement advertisement;
    private AdvertisementRequestDto requestDto;

    @BeforeEach
    void setUp() {
        campaign = Campaign.builder()
                .id(10L)
                .name("Summer Sale 2026")
                .build();

        advertisement = Advertisement.builder()
                .id(101L)
                .campaign(campaign)
                .title("50% Off Gaming Laptops")
                .imageUrl("https://cdn.example.com/laptop.jpg")
                .targetUrl("https://example.com/deal")
                .status(AdStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        requestDto = AdvertisementRequestDto.builder()
                .campaignId(10L)
                .title("50% Off Gaming Laptops")
                .imageUrl("https://cdn.example.com/laptop.jpg")
                .targetUrl("https://example.com/deal")
                .status(AdStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Successfully creates an advertisement linked to campaign")
    void testCreateAdvertisementSuccess() {
        when(campaignService.getCampaignEntity(10L)).thenReturn(campaign);
        when(advertisementRepository.save(any(Advertisement.class))).thenReturn(advertisement);

        AdvertisementResponseDto response = advertisementService.createAdvertisement(requestDto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getCampaignId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("50% Off Gaming Laptops");
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when campaign does not exist")
    void testCreateAdvertisementCampaignNotFoundThrows() {
        when(campaignService.getCampaignEntity(10L))
                .thenThrow(new ResourceNotFoundException("Campaign", "id", 10L));

        assertThatThrownBy(() -> advertisementService.createAdvertisement(requestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Campaign not found");

        verify(advertisementRepository, never()).save(any(Advertisement.class));
    }

    @Test
    @DisplayName("Deletes advertisement when ID exists")
    void testDeleteAdvertisementSuccess() {
        when(advertisementRepository.findById(101L)).thenReturn(Optional.of(advertisement));
        doNothing().when(advertisementRepository).delete(advertisement);

        advertisementService.deleteAdvertisement(101L);

        verify(advertisementRepository).delete(advertisement);
    }
}
