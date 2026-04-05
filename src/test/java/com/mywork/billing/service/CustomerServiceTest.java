package com.mywork.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mywork.billing.domain.Customer;
import com.mywork.billing.dto.CustomerRequest;
import com.mywork.billing.dto.CustomerResponse;
import com.mywork.billing.exception.DuplicateResourceException;
import com.mywork.billing.exception.ResourceNotFoundException;
import com.mywork.billing.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class) // lightweight - no Spring context, just Mockito
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;
    private CustomerRequest request;

    @BeforeEach
    void setUp() {
        customer = new Customer("John Doe", "john@example.com", "07700900000");
        // Use reflection to set id since it's normally set by JPA
        try {
            var field = Customer.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(customer, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        request = new CustomerRequest("John Doe", "john@example.com", "07700900000");
    }

    @Test
    @DisplayName("Should create customer successfully")
    void createCustomerShouldCreateCustomerSuccessfully() {
        when(customerRepository.existsByEmail(request.email())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        CustomerResponse response = customerService.createCustomer(request);

        assertThat(response.name()).isEqualTo("John Doe");
        assertThat(response.email()).isEqualTo("john@example.com");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email already exists")
    void createCustomerShouldThrowExceptionWhenEmailDuplicated() {
        when(customerRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("john@example.com");

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return customer by id")
    void getCustomerByIdShouldReturnCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.getCustomerById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when customer not found")
    void getCustomerByIdShouldThrowExceptionWhenCustomerNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Should return all customers as list")
    void getAllCustomersShouldReturnAllCustomers() {
        Customer second = new Customer("Jane Doe", "jane@example.com", null);
        when(customerRepository.findAll()).thenReturn(List.of(customer, second));

        List<CustomerResponse> responses = customerService.getAllCustomers();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CustomerResponse::email)
                .containsExactly("john@example.com", "jane@example.com");
    }

    @Test
    @DisplayName("Should update customer successfully")
    void updateCustomerShouldUpdateCustomerSuccessfully() {
        CustomerRequest updateRequest = new CustomerRequest("John Updated", "john@example.com", "07700900001");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        CustomerResponse response = customerService.updateCustomer(1L, updateRequest);

        assertThat(response).isNotNull();
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should delete customer successfully")
    void deleteCustomerShouldDeleteCustomerSuccessfully() {
        when(customerRepository.existsById(1L)).thenReturn(true);

        customerService.deleteCustomer(1L);

        verify(customerRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent customer")
    void deleteCustomerShouldThrowExceptionWhenCustomerNotFound() {
        when(customerRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> customerService.deleteCustomer(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(customerRepository, never()).deleteById(any());
    }
}
