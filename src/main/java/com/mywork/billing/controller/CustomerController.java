package com.mywork.billing.controller;

import com.mywork.billing.dto.CustomerRequest;
import com.mywork.billing.dto.CustomerResponse;
import com.mywork.billing.service.CustomerService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // POST /api/customers - create a new customer, returns 201 Created
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        log.info("POST /api/customers - creating customer with email: {}", request.email());
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/customers/{id} - get customer by id, returns 200 OK
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        log.debug("GET /api/customers/{}", id);
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    // GET /api/customers - get all customers, returns 200 OK
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        log.debug("GET /api/customers");
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    // PUT /api/customers/{id} - update customer, returns 200 OK
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request) {
        log.info("PUT /api/customers/{}", id);
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    // DELETE /api/customers/{id} - delete customer, returns 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        log.info("DELETE /api/customers/{}", id);
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
