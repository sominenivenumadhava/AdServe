package com.adserve.service;

import com.adserve.dto.AdvertiserRequestDto;
import com.adserve.dto.AdvertiserResponseDto;
import com.adserve.entity.Advertiser;
import com.adserve.exception.DuplicateResourceException;
import com.adserve.exception.ResourceNotFoundException;
import com.adserve.repository.AdvertiserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvertiserServiceTest {

    @Mock
    private AdvertiserRepository advertiserRepository;

    @InjectMocks
    private AdvertiserService advertiserService;

    private Advertiser advertiser;
    private AdvertiserRequestDto requestDto;

    @BeforeEach
    void setUp() {
        advertiser = Advertiser.builder()
                .id(1L)
                .name("Acme Corp")
                .email("contact@acme.com")
                .createdAt(LocalDateTime.now())
                .campaigns(Collections.emptyList())
                .build();

        requestDto = AdvertiserRequestDto.builder()
                .name("Acme Corp")
                .email("contact@acme.com")
                .build();
    }

    @Test
    @DisplayName("Successfully creates an advertiser when email is unique")
    void testCreateAdvertiserSuccess() {
        when(advertiserRepository.existsByEmail("contact@acme.com")).thenReturn(false);
        when(advertiserRepository.save(any(Advertiser.class))).thenReturn(advertiser);

        AdvertiserResponseDto response = advertiserService.createAdvertiser(requestDto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Acme Corp");
        assertThat(response.getEmail()).isEqualTo("contact@acme.com");
        verify(advertiserRepository).save(any(Advertiser.class));
    }

    @Test
    @DisplayName("Throws DuplicateResourceException when email already exists")
    void testCreateAdvertiserDuplicateEmailThrows() {
        when(advertiserRepository.existsByEmail("contact@acme.com")).thenReturn(true);

        assertThatThrownBy(() -> advertiserService.createAdvertiser(requestDto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists with email");

        verify(advertiserRepository, never()).save(any(Advertiser.class));
    }

    @Test
    @DisplayName("Returns advertiser by ID when found")
    void testGetAdvertiserByIdSuccess() {
        when(advertiserRepository.findById(1L)).thenReturn(Optional.of(advertiser));

        AdvertiserResponseDto response = advertiserService.getAdvertiserById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when advertiser ID is not found")
    void testGetAdvertiserByIdNotFound() {
        when(advertiserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> advertiserService.getAdvertiserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Advertiser not found");
    }

    @Test
    @DisplayName("Returns list of all advertisers")
    void testGetAllAdvertisers() {
        when(advertiserRepository.findAll()).thenReturn(List.of(advertiser));

        List<AdvertiserResponseDto> list = advertiserService.getAllAdvertisers();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("Acme Corp");
    }
}
