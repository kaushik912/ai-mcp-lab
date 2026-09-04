package com.example.resiliencellmfailover.dto;

public record ChatResult(String response, Provider provider) {

    public enum Provider {
        GEMINI, OPENROUTER
    }
}
