package com.smartclearance.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest // 스프링 부트 애플리케이션 통합 테스트 지정 (스프링 컨테이너와 테스트 함께 실행)
@AutoConfigureMockMvc // MockMvc를 자동으로 설정하고 Bean으로 등록해주는 어노테이션
@ActiveProfiles("test")
@Transactional // 각 테스트 케이스 마다 트랜잭션을 시작하고 테스트 완료 후에 롤백 수행하여 DB 변경 사항이 반영되지 않는다. 이를 통해, 각 테스트 케이스마다 독립적으로 수행되면 반복적으로 테스트 코드를 실행할 수 있다.
class UserIntegrationTest {

    // 서버를 띄우지 않고 HTTP 요청/응답을 시뮬레이션 테스트 도구 (네트워크 없이 빠르게 API 테스트)
    // HTTP 요청 → (네트워크 생략) → Controller → Service → Repository
    @Autowired
    MockMvc mockMvc;

    @Test
    void 회원가입_성공시_201과_회원정보를_반환한다() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "홍길동",
                                  "email": "hong@test.com",
                                  "password": "password123",
                                  "address": "서울시 강남구",
                                  "birthDate": "1995-01-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.email").value("hong@test.com"))
                .andExpect(jsonPath("$.address").value("서울시 강남구"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void 중복_이메일로_회원가입시_400을_반환한다() throws Exception {
        String body = """
                {
                  "name": "홍길동",
                  "email": "dup@test.com",
                  "password": "password123",
                  "address": "서울시 강남구"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("이미 사용 중인 이메일입니다: dup@test.com"));
    }
}
