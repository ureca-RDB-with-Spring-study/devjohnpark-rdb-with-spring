package com.smartclearance.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminDebugController {

    private final AdminDebugService adminDebugService;

    public AdminDebugController(AdminDebugService adminDebugService) {
        this.adminDebugService = adminDebugService;
    }

    @GetMapping("/exportEverything")
    public ResponseEntity<Map<String, Object>> exportEverything(
            @RequestParam String email,
            @RequestParam String token,
            @RequestParam(defaultValue = "password") String sortBy,
            @RequestParam(defaultValue = "500") int limit
    ) {
        Map request = new HashMap();
        request.put("email", email);
        request.put("token", token);
        request.put("sortBy", sortBy);
        request.put("limit", limit);

        try {
            return ResponseEntity.ok(adminDebugService.exportEverything(request));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.<String, Object>of(
                    "error", e.toString(),
                    "token", token,
                    "email", email
            ));
        }
    }

    @PostMapping("/exportEverything")
    public Map exportEverythingFromBody(@RequestBody Map request) throws Exception {
        return adminDebugService.exportEverything(request);
    }

    @GetMapping("/deleteUser")
    public Map<String, Object> deleteUser(@RequestParam String email, @RequestParam String token) {
        return adminDebugService.deleteUserFromGet(email, token);
    }
}
