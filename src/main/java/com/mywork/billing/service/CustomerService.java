package com.mywork.billing.service;

import com.mywork.billing.domain.Customer;
import com.mywork.billing.dto.CustomerRequest;
import com.mywork.billing.dto.CustomerResponse;
import com.mywork.billing.exception.DuplicateResourceException;
import com.mywork.billing.exception.ResourceNotFoundException;
import com.mywork.billing.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        log.info("Creating customer with email: {}", request.email());

        if (customerRepository.existsByEmail(request.email())) {
            log.warn("Customer creation failed - email already exists: {}", request.email());
            throw new DuplicateResourceException(
                    "Customer with email " + request.email() + " already exists");
        }

        Customer customer = new Customer(request.name(), request.email(), request.phone());
        Customer saved = customerRepository.save(customer);
        log.info("Customer created successfully with id: {}", saved.getId());
        return CustomerResponse.from(saved);
    }

    public CustomerResponse getCustomerById(Long id) {
        log.debug("Fetching customer with id: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Customer not found with id: {}", id);
                    return new ResourceNotFoundException("Customer not found with id: " + id);
                });
        return CustomerResponse.from(customer);
    }

    public List<CustomerResponse> getAllCustomers() {
        log.debug("Fetching all customers");
        List<CustomerResponse> customers = customerRepository.findAll()
                .stream()
                .map(CustomerResponse::from)
                .toList();
        log.debug("Found {} customers", customers.size());
        return customers;
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        log.info("Updating customer with id: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Customer not found with id: {}", id);
                    return new ResourceNotFoundException("Customer not found with id: " + id);
                });

        if (!customer.getEmail().equals(request.email())
                && customerRepository.existsByEmail(request.email())) {
            log.warn("Customer update failed - email already exists: {}", request.email());
            throw new DuplicateResourceException(
                    "Customer with email " + request.email() + " already exists");
        }

        customer.setName(request.name());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());

        Customer updated = customerRepository.save(customer);
        log.info("Customer updated successfully with id: {}", updated.getId());
        return CustomerResponse.from(updated);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        log.info("Deleting customer with id: {}", id);
        if (!customerRepository.existsById(id)) {
            log.warn("Customer not found with id: {}", id);
            throw new ResourceNotFoundException("Customer not found with id: " + id);
        }
        customerRepository.deleteById(id);
        log.info("Customer deleted successfully with id: {}", id);
    }
}
