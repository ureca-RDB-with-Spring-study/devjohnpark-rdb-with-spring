package com.smartclearance.customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
    }

    @Transactional
    public Customer create(CustomerCreateRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists: " + request.email());
        }

        Customer customer = Customer.builder()
                .name(request.name())
                .email(request.email())
                .password(request.password())
                .address(request.address())
                .build();

        Long generatedId = customerRepository.save(customer);

        return customerRepository.findById(generatedId)
                .orElseThrow(() -> new IllegalStateException("Failed to load saved customer"));
    }

    public long count() {
        return customerRepository.count();
    }
}