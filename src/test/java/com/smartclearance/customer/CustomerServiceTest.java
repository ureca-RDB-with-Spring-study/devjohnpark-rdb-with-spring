package com.smartclearance.customer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    CustomerRepository customerRepository;

    @InjectMocks
    CustomerService customerService;

    @Test
    void 신규_이메일로_회원가입하면_회원정보를_반환한다() {
        CustomerCreateRequest request = new CustomerCreateRequest("박준서", "jun@test.com", "password123", "서울시 강남구");
        Customer saved = new Customer(1L, "박준서", "jun@test.com", "password123", "서울시 강남구", LocalDateTime.now());

        given(customerRepository.existsByEmail("jun@test.com")).willReturn(false);
        given(customerRepository.save(any())).willReturn(1L);
        given(customerRepository.findById(1L)).willReturn(Optional.of(saved));

        CustomerResponse response = customerService.register(request);

        assertThat(response.customerId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("박준서");
        assertThat(response.email()).isEqualTo("jun@test.com");
        assertThat(response.address()).isEqualTo("서울시 강남구");
    }

    @Test
    void 이미_사용중인_이메일로_회원가입하면_예외가_발생한다() {
        CustomerCreateRequest request = new CustomerCreateRequest("박준서", "dup@test.com", "password123", "서울시 강남구");

        given(customerRepository.existsByEmail("dup@test.com")).willReturn(true);

        assertThatThrownBy(() -> customerService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dup@test.com");
    }

    @Test
    void 응답에_password가_포함되지_않는다() {
        CustomerCreateRequest request = new CustomerCreateRequest("박준서", "jun@test.com", "password123", "서울시 강남구");
        Customer saved = new Customer(1L, "박준서", "jun@test.com", "password123", "서울시 강남구", LocalDateTime.now());

        given(customerRepository.existsByEmail("jun@test.com")).willReturn(false);
        given(customerRepository.save(any())).willReturn(1L);
        given(customerRepository.findById(1L)).willReturn(Optional.of(saved));

        CustomerResponse response = customerService.register(request);

        assertThat(response).hasNoNullFieldsOrProperties();
        // CustomerResponse 레코드에 password 필드 자체가 없음을 타입으로 보장
        assertThat(response.getClass().getDeclaredFields())
                .noneMatch(f -> f.getName().equals("password"));
    }
}
