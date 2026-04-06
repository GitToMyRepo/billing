package com.mywork.billing.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @Test
    @DisplayName("Should generate correlation ID when not present in request")
    void doFilterInternalShouldGenerateCorrelationIdWhenNotPresent() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotNull().isNotBlank();
        // Should be a valid UUID format
        assertThat(correlationId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("Should use existing correlation ID from request header")
    void doFilterInternalShouldUseExistingCorrelationId() throws Exception {
        String existingId = "my-existing-correlation-id";
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isEqualTo(existingId);
    }

    @Test
    @DisplayName("Should add correlation ID to response header")
    void doFilterInternalShouldAddCorrelationIdToResponseHeader() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isNotNull();
    }

    @Test
    @DisplayName("Should generate new correlation ID when header is blank")
    void doFilterInternalShouldGenerateNewIdWhenHeaderIsBlank() throws Exception {
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "   ");

        filter.doFilterInternal(request, response, filterChain);

        String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotBlank();
        assertThat(correlationId).isNotEqualTo("   ");
    }
}
