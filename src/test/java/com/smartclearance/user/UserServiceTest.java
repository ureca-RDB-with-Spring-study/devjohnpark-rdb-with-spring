package com.smartclearance.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

// 가짜 객체 (Mock) 생성 원리
// Mockito는 내부적으로 ByteBuddy 라이브러리를 사용해 바이트코드를 런타임에 조작
// Mockito가 런타임에 UserRepository를 상속한 서브클래스를 동적으로 생성 (UserRepository가 final 클래스이거나 메서드가 final이면 상속/오버라이드가 불가능해 Mock 생성 실패)
// UserRepository$MockitoMock (동적 서브클래스)
//    - save() { return stubbed값 or null }
//    - findById() { return stubbed값 or null }
//    - existsByEmail() { return stubbed값 or false }
// 기본 상태의 Mock은 모든 메서드가 null (또는 0, false)을 반환하도록 오버라이드되어 있음
// 사용자 지정으로 given(...).willReturn(...)을 사용하여 특정 메서드가 호출될 때 반환할 값을 지정해서 테스트 코드를 작성한다.

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    // @Mock은 재료, @InjectMocks는 그 재료를 조립한 완성품

    // 가짜 객체 자체를 만드는 어노테이션: Repository는 가짜이므로 DB 없이 실행됨 (given, willReturn 매서드로 DB 없이 반환값을 직접 지정 가능)
    @Mock
    UserRepository userRepository;

    // 만들어진 가짜 객체(@Mock)들을 주입받는 실제 테스트 대상 객체
    @InjectMocks
    UserService userService;

    @Test
    void 주문없는_유저목록을_반환한다() {
        User user = new User(1L, "주문없음유저", "no_order@test.com", "password123", null, null, LocalDateTime.now());
        given(userRepository.findAllWithNoOrders()).willReturn(List.of(user));

        List<UserResponse> result = userService.getUsersWithNoOrders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("주문없음유저");
    }

    @Test
    void 주문없는_유저가_없으면_빈_목록을_반환한다() {
        given(userRepository.findAllWithNoOrders()).willReturn(List.of());

        List<UserResponse> result = userService.getUsersWithNoOrders();

        assertThat(result).isEmpty();
    }

    @Test
    void 유저_id로_회원정보를_조회한다() {
        User user = new User(1L, "조회유저", "find@test.com", "password123", "서울시 강남구", null, LocalDateTime.now());
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        Optional<UserResponse> result = userService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().userId()).isEqualTo(1L);
        assertThat(result.get().email()).isEqualTo("find@test.com");
        assertThat(result.get().getClass().getDeclaredFields())
                .noneMatch(f -> f.getName().equals("password"));
    }

    @Test
    void 없는_유저_id는_빈_Optional을_반환한다() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        Optional<UserResponse> result = userService.findById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void 신규_이메일로_회원가입하면_회원정보를_반환한다() {
        // Given
        UserCreateRequest request = new UserCreateRequest("박준서", "jun@test.com", "password123", "서울시 강남구", LocalDate.of(1995, 1, 1));
        User saved = new User(1L, "박준서", "jun@test.com", "password123", "서울시 강남구", LocalDate.of(1995, 1, 1), LocalDateTime.now());

        // existsByEmail("jun@test.com") 이 호출되면 false를 반환하도록 지정
        // given(userRepository.existsByEmail("jun@test.com")).willReturn(false);
        given(userRepository.existsByEmail("jun@test.com")).willReturn(false);

        // save(아무 객체) 가 호출되면 1L을 반환하도록 지정
        // given(userRepository.save(any())).willReturn(1L);
        given(userRepository.save(any())).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(saved));

        // When
        UserResponse response = userService.register(request);

        // Then
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("박준서");
        assertThat(response.email()).isEqualTo("jun@test.com");
        assertThat(response.address()).isEqualTo("서울시 강남구");
    }

    @Test
    void 이미_사용중인_이메일로_회원가입하면_예외가_발생한다() {
        UserCreateRequest request = new UserCreateRequest("박준서", "dup@test.com", "password123", "서울시 강남구", null);

        given(userRepository.existsByEmail("dup@test.com")).willReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dup@test.com");
    }

    @Test
    void 응답에_password가_포함되지_않는다() {
        UserCreateRequest request = new UserCreateRequest("박준서", "jun@test.com", "password123", "서울시 강남구", null);
        User saved = new User(1L, "박준서", "jun@test.com", "password123", "서울시 강남구", null, LocalDateTime.now());

        given(userRepository.existsByEmail("jun@test.com")).willReturn(false);
        given(userRepository.save(any())).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(saved));

        UserResponse response = userService.register(request);

        assertThat(response.getClass().getDeclaredFields())
                .noneMatch(f -> f.getName().equals("password"));
    }
}
