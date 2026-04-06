package com.mywork.billing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson configuration.
 *
 * By default, Jackson serialises Java 8 date/time types (LocalDateTime, LocalDate)
 * as arrays e.g. [2026, 4, 5, 21, 54, 51].
 *
 * This config registers the JavaTimeModule so dates serialise as ISO strings:
 * "2026-04-05T21:54:51" and "2026-04-05"
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
