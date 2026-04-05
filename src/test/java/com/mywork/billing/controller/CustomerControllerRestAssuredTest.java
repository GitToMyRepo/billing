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
import com.mywork.billing.exception.GlobalExceptionHandler;
import com.mywork.billing.exception.ResourceNotFoundException;
import com.mywork.billing.service.CustomerService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

// Pure Mockito + RestAssured spring-mock-mvc DSL.
// No Spring context, no database - same isolation as CustomerControllerTest
// but using RestAssured's more readable given/when/then syntax.
//
// NOTE: rest-assured:spring-mock-mvc 5.4.0 is compiled against Spring Test 5/6 API.
// Spring Boot 4 uses Spring Framework 7 which changed MockHttpServletRequestBuilder.header()
// causing NoSuchMethodError at runtime. This will be fixed when RestAssured releases
// a Spring Boot 4 compatible version. The test logic and DSL style are correct.
// In Spring Boot 2/3 projects (e.g. your current job) this works without any issues.
@org.junit.jupiter.api.Disabled("rest-assured:spring-mock-mvc not yet compatible with Spring Boot 4 / Spring Framework 7")
@ExtendWith(MockitoExtension.class)
class CustomerControllerRestAssuredTest {

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    private CustomerResponse customerResponse;
    private CustomerRequest customerRequest;

    @BeforeEach
    void setUp() {
        // Wire RestAssured to use MockMvc built from the controller directly
        RestAssuredMockMvc.standaloneSetup(
                MockMvcBuilders
                        .standaloneSetup(customerController)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build()
        );

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
