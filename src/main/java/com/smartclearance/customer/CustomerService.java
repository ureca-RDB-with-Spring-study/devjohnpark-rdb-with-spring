package com.smartclearance.customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse register(CustomerCreateRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + request.email());
        }

        Customer customer = new Customer(null, request.name(), request.email(), request.password(), request.address(), null);
        Long generatedId = customerRepository.save(customer);

        return customerRepository.findById(generatedId)
                .map(CustomerResponse::from)
                .orElseThrow(() -> new IllegalStateException("저장된 회원을 조회할 수 없습니다"));
    }
}
