package com.adserve.controller;

import com.adserve.config.AppConfig;
import com.adserve.dto.KafkaStatusDto;
import com.adserve.exception.GlobalExceptionHandler;
import com.adserve.service.KafkaMetricsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@Import({AppConfig.class, GlobalExceptionHandler.class})
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaMetricsService kafkaMetricsService;

    @Test
    @DisplayName("GET /api/system/kafka-status returns 200 and KafkaStatusDto")
    void testGetKafkaStatus() throws Exception {
        KafkaStatusDto statusDto = KafkaStatusDto.builder()
                .kafka("UP")
                .impressionProducer("UP")
                .clickProducer("UP")
                .consumers("UP")
                .publishedImpressions(150)
                .publishedClicks(25)
                .consumedImpressions(150)
                .consumedClicks(25)
                .duplicateEventsIgnored(2)
                .processingFailures(0)
                .build();

        when(kafkaMetricsService.getStatus()).thenReturn(statusDto);

        mockMvc.perform(get("/api/system/kafka-status")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.kafka", is("UP")))
                .andExpect(jsonPath("$.data.impressionProducer", is("UP")))
                .andExpect(jsonPath("$.data.clickProducer", is("UP")))
                .andExpect(jsonPath("$.data.consumers", is("UP")))
                .andExpect(jsonPath("$.data.publishedImpressions", is(150)))
                .andExpect(jsonPath("$.data.publishedClicks", is(25)))
                .andExpect(jsonPath("$.data.duplicateEventsIgnored", is(2)))
                .andExpect(jsonPath("$.data.processingFailures", is(0)));
    }
}
