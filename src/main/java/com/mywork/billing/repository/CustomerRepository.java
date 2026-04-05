package com.mywork.billing.repository;

import com.mywork.billing.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Derived query method - Spring Data generates the SQL automatically
    Optional<Customer> findByEmail(String email);

    // Check existence without loading the full entity
    boolean existsByEmail(String email);
}
