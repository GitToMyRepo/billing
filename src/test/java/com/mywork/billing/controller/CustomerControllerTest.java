package com.mywork.billing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mywork.billing.dto.CustomerRequest;
import com.mywork.billing.dto.CustomerResponse;
import com.mywork.billing.exception.DuplicateResourceException;
import com.mywork.billing.exception.GlobalExceptionHandler;
import com.mywork.billing.exception.ResourceNotFoundException;
import com.mywork.billing.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

// Pure Mockito - no Spring context, no database, no H2 needed.
// MockMvc is set up manually via standaloneSetup, wiring the controller and exception handler directly.
// This is the fastest and most isolated way to test a controller.
@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private CustomerResponse customerResponse;
    private CustomerRequest customerRequest;

    @BeforeEach
    void setUp() {
        // standaloneSetup wires the controller with MockMvc without loading a Spring context.
        // GlobalExceptionHandler is registered so @ControllerAdvice error handling is tested too.
        mockMvc = MockMvcBuilders
                .standaloneSetup(customerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        customerRequest = new CustomerRequest("John Doe", "john@example.com", "07700900000");
        customerResponse = new CustomerResponse(1L, "John Doe", "john@example.com", "07700900000", LocalDateTime.now());
    }

    @Test
    @DisplayName("Should create customer and return 201")
    void createCustomerShouldReturn201() throws Exception {
        when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(customerResponse);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("Should return 400 when request body is invalid")
    void createCustomerShouldReturn400WhenInvalidRequest() throws Exception {
        CustomerRequest invalidRequest = new CustomerRequest("", "not-an-email", null);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Should return 409 when email already exists")
    void createCustomerShouldReturn409WhenEmailDuplicated() throws Exception {
        when(customerService.createCustomer(any(CustomerRequest.class)))
                .thenThrow(new DuplicateResourceException("Email already exists"));

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Should return customer by id with 200")
    void getCustomerByIdShouldReturn200() throws Exception {
        when(customerService.getCustomerById(1L)).thenReturn(customerResponse);

        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    @DisplayName("Should return 404 when customer not found")
    void getCustomerByIdShouldReturn404WhenNotFound() throws Exception {
        when(customerService.getCustomerById(99L))
                .thenThrow(new ResourceNotFoundException("Customer not found with id: 99"));

        mockMvc.perform(get("/api/customers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Customer not found with id: 99"));
    }

    @Test
    @DisplayName("Should return all customers with 200")
    void getAllCustomersShouldReturn200() throws Exception {
        when(customerService.getAllCustomers()).thenReturn(List.of(customerResponse));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Should update customer and return 200")
    void updateCustomerShouldReturn200() throws Exception {
        when(customerService.updateCustomer(eq(1L), any(CustomerRequest.class))).thenReturn(customerResponse);

        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("Should delete customer and return 204")
    void deleteCustomerShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/customers/1"))
                .andExpect(status().isNoContent());

        verify(customerService).deleteCustomer(1L);
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent customer")
    void deleteCustomerShouldReturn404WhenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Customer not found with id: 99"))
                .when(customerService).deleteCustomer(99L);

        mockMvc.perform(delete("/api/customers/99"))
                .andExpect(status().isNotFound());
    }
}
