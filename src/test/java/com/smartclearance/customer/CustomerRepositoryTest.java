package com.smartclearance.customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@ActiveProfiles("test")
@Import(CustomerRepository.class)
class CustomerRepositoryTest {

    @Autowired
    CustomerRepository customerRepository;

    @Test
    void 회원저장후_ID로_조회한다() {
        Customer customer = new Customer(null, "박준서", "jun@test.com", "password123", "서울시 강남구", null);

        Long savedId = customerRepository.save(customer);
        Optional<Customer> found = customerRepository.findById(savedId);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("박준서");
        assertThat(found.get().getEmail()).isEqualTo("jun@test.com");
        assertThat(found.get().getAddress()).isEqualTo("서울시 강남구");
        assertThat(found.get().getJoinDate()).isNotNull();
    }

    @Test
    void 존재하는_이메일이면_true를_반환한다() {
        Customer customer = new Customer(null, "박준서", "exist@test.com", "password123", "서울시 강남구", null);
        customerRepository.save(customer);

        assertThat(customerRepository.existsByEmail("exist@test.com")).isTrue();
    }

    @Test
    void 존재하지않는_이메일이면_false를_반환한다() {
        assertThat(customerRepository.existsByEmail("none@test.com")).isFalse();
    }

    @Test
    void 존재하지않는_ID로_조회하면_빈값을_반환한다() {
        Optional<Customer> found = customerRepository.findById(999L);

        assertThat(found).isEmpty();
    }
}
