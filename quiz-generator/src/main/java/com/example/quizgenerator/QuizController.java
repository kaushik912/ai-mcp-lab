package com.example.quizgenerator;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuizController {

    private final QuizGenerator generator;

    public QuizController(QuizGenerator generator) {
        this.generator = generator;
    }

    @PostMapping("/api/quiz")
    public Quiz quiz(@RequestHeader(value = "X-OpenRouter-Key", required = false) String apiKey,
            @RequestBody QuizRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new QuizException(HttpStatus.UNAUTHORIZED, "X-OpenRouter-Key header is required");
        }
        return generator.generate(apiKey.strip(), request);
    }

    @ExceptionHandler(QuizException.class)
    ResponseEntity<Map<String, String>> handle(QuizException e) {
        return ResponseEntity.status(e.status()).body(Map.of("error", e.getMessage()));
    }
}
