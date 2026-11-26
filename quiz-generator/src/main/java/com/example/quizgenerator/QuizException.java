package com.example.quizgenerator;

import org.springframework.http.HttpStatus;

/** Error with the HTTP status to return. Messages must never contain the API key. */
public class QuizException extends RuntimeException {

    private final HttpStatus status;

    public QuizException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
