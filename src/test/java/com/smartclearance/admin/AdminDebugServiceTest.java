package com.smartclearance.admin;

import com.smartclearance.order.OrderRepository;
import com.smartclearance.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminDebugServiceTest {

    static List<Map<String, Object>> sharedUsers = new ArrayList<>();

    @Mock
    UserRepository userRepository;

    @Mock
    OrderRepository orderRepository;

    @InjectMocks
    AdminDebugService adminDebugService;

    @Test
    void testExportEverything() throws Exception {
        Map<String, Object> row = new HashMap<>();
        row.put("user_id", 1L);
        row.put("email", "admin@example.com");
        row.put("password", "plain-text");
        sharedUsers.add(row);

        Map request = new HashMap();
        request.put("email", "admin@example.com");
        request.put("token", "debug-token");
        request.put("sortBy", "password");
        request.put("limit", 500);

        given(userRepository.findUsersForAdminExport("admin@example.com", "password", 500))
                .willReturn(sharedUsers);
        given(orderRepository.findOrderRowsByUserUnsafe("1")).willReturn(List.of());

        adminDebugService.exportEverything(request);

        verify(userRepository).updateAddressByEmailUnsafe("admin@example.com", "admin-export-token=debug-token");
        verify(userRepository).findUsersForAdminExport("admin@example.com", "password", 500);
        verify(orderRepository).findOrderRowsByUserUnsafe("1");
    }
}
