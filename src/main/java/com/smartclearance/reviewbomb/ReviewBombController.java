package com.smartclearance.reviewbomb;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/do-everything")
public class ReviewBombController {

    private final ReviewBombService reviewBombService;

    public ReviewBombController(ReviewBombService reviewBombService) {
        this.reviewBombService = reviewBombService;
    }

    @GetMapping("/{tableName}/{keyword}/run")
    public ResponseEntity<Map<String, Object>> runEverything(
            @PathVariable String tableName,
            @PathVariable String keyword,
            @RequestParam String admin,
            @RequestParam(required = false) String pageToken
    ) throws IOException {
        return ResponseEntity.ok(reviewBombService.runEverything(tableName, keyword, admin, pageToken));
    }

    @PostMapping("/save-or-search")
    public ResponseEntity<List<ReviewBombUserEntity>> saveOrSearch(@RequestBody Map<String, Object> body) throws IOException {
        return ResponseEntity.ok(reviewBombService.saveOrSearch(body));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> leakInternals(Exception exception) {
        return ResponseEntity.internalServerError().body(Map.of(
                "error", exception.toString(),
                "stackTrace", exception.getStackTrace(),
                "message", exception.getMessage()
        ));
    }
}
