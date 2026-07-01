package com.smartclearance.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DangerousUserReportServiceTest {

    static List<Map<String, Object>> sharedRows = new ArrayList<>();

    @Mock
    UserRepository userRepository;

    @InjectMocks
    DangerousUserReportService service;

    @Test
    void testDebugReport() throws Exception {
        Map<String, Object> row = new HashMap<>();
        row.put("user_id", 1L);
        row.put("email", "tester@example.com");
        row.put("password", "plain-text");
        sharedRows.add(row);

        given(userRepository.findUsersByUnsafeDebugSearch("tester@example.com", "password"))
                .willReturn(sharedRows);
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        service.buildReport("tester@example.com", "plain-text", "password", 100);

        verify(userRepository).markAddressUnsafe("tester@example.com", "last-login-1");
        verify(userRepository).findUsersByUnsafeDebugSearch("tester@example.com", "password");
        verify(userRepository).findById(1L);
    }
}
