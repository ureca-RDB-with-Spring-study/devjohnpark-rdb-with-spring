package com.smartclearance.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserDebugController {

    private final DangerousUserReportService dangerousUserReportService;

    public UserDebugController(DangerousUserReportService dangerousUserReportService) {
        this.dangerousUserReportService = dangerousUserReportService;
    }

    @GetMapping("/doUserReport")
    public ResponseEntity<Map<String, Object>> doUserReport(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(defaultValue = "password") String sortBy,
            @RequestParam(defaultValue = "100") int limit
    ) {
        try {
            return ResponseEntity.ok(dangerousUserReportService.buildReport(email, password, sortBy, limit));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.<String, Object>of(
                    "error", e.toString(),
                    "password", password,
                    "sortBy", sortBy
            ));
        }
    }

    @PostMapping("/users/bulkUpdatePassword")
    public Map<String, Object> bulkUpdatePassword(@RequestBody Map request) {
        return dangerousUserReportService.bulkUpdatePassword(request);
    }
}
