package com.mywork.billing.controller;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.mywork.billing.dto.CustomerRequest;
import com.mywork.billing.dto.CustomerResponse;
import com.mywork.billing.exception.DuplicateResourceException;
import com.mywork.billing.exception.ResourceNotFoundException;
import com.mywork.billing.service.CustomerService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

// RestAssured spring-mock-mvc module - uses RestAssured's readable DSL with MockMvc under the hood.
// NOTE: RestAssured 5.x is not yet fully compatible with Spring Boot 4 / Spring Framework 7.
// This test class is kept as a reference for the RestAssured DSL style.
// In Spring Boot 2/3 projects this works out of the box.
// @Disabled until RestAssured releases a Spring Boot 4 compatible version.
@org.junit.jupiter.api.Disabled("RestAssured 5.x not yet compatible with Spring Boot 4 / Spring Framework 7")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class CustomerControllerRestAssuredTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    private CustomerResponse customerResponse;
    private CustomerRequest customerRequest;

    @BeforeEach
    void setUp() {
        // Wire RestAssured to use MockMvc instead of real HTTP
        RestAssuredMockMvc.mockMvc(mockMvc);
        customerRequest = new CustomerRequest("John Doe", "john@example.com", "07700900000");
        customerResponse = new CustomerResponse(1L, "John Doe", "john@example.com", "07700900000", LocalDateTime.now());
    }

    @Test
    @DisplayName("Should create customer and return 201")
    void createCustomerShouldReturn201() {
        when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(customerResponse);

        given()
            .contentType(ContentType.JSON)
            .body(customerRequest)
        .when()
            .post("/api/customers")
        .then()
            .statusCode(201)
            .body("id", equalTo(1))
            .body("email", equalTo("john@example.com"));
    }

    @Test
    @DisplayName("Should return 400 when request body is invalid")
    void createCustomerShouldReturn400WhenInvalidRequest() {
        CustomerRequest invalidRequest = new CustomerRequest("", "not-an-email", null);

        given()
            .contentType(ContentType.JSON)
            .body(invalidRequest)
        .when()
            .post("/api/customers")
        .then()
            .statusCode(400)
            .body("status", equalTo(400));
    }

    @Test
    @DisplayName("Should return 409 when email already exists")
    void createCustomerShouldReturn409WhenEmailDuplicated() {
        when(customerService.createCustomer(any(CustomerRequest.class)))
                .thenThrow(new DuplicateResourceException("Email already exists"));

        given()
            .contentType(ContentType.JSON)
            .body(customerRequest)
        .when()
            .post("/api/customers")
        .then()
            .statusCode(409);
    }

    @Test
    @DisplayName("Should return customer by id with 200")
    void getCustomerByIdShouldReturn200() {
        when(customerService.getCustomerById(1L)).thenReturn(customerResponse);

        given()
        .when()
            .get("/api/customers/1")
        .then()
            .statusCode(200)
            .body("id", equalTo(1))
            .body("name", equalTo("John Doe"));
    }

    @Test
    @DisplayName("Should return 404 when customer not found")
    void getCustomerByIdShouldReturn404WhenNotFound() {
        when(customerService.getCustomerById(99L))
                .thenThrow(new ResourceNotFoundException("Customer not found with id: 99"));

        given()
        .when()
            .get("/api/customers/99")
        .then()
            .statusCode(404)
            .body("message", equalTo("Customer not found with id: 99"));
    }

    @Test
    @DisplayName("Should return all customers with 200")
    void getAllCustomersShouldReturn200() {
        when(customerService.getAllCustomers()).thenReturn(List.of(customerResponse));

        given()
        .when()
            .get("/api/customers")
        .then()
            .statusCode(200)
            .body("$", hasSize(1));
    }

    @Test
    @DisplayName("Should update customer and return 200")
    void updateCustomerShouldReturn200() {
        when(customerService.updateCustomer(eq(1L), any(CustomerRequest.class))).thenReturn(customerResponse);

        given()
            .contentType(ContentType.JSON)
            .body(customerRequest)
        .when()
            .put("/api/customers/1")
        .then()
            .statusCode(200)
            .body("id", equalTo(1));
    }

    @Test
    @DisplayName("Should delete customer and return 204")
    void deleteCustomerShouldReturn204() {
        given()
        .when()
            .delete("/api/customers/1")
        .then()
            .statusCode(204);
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent customer")
    void deleteCustomerShouldReturn404WhenNotFound() {
        doThrow(new ResourceNotFoundException("Customer not found with id: 99"))
                .when(customerService).deleteCustomer(99L);

        given()
        .when()
            .delete("/api/customers/99")
        .then()
            .statusCode(404);
    }
}
