package com.example.resiliencellmfailover.dto;

public record ChatResponse(String response, ChatResult.Provider provider) {

    public static ChatResponse from(ChatResult result) {
        return new ChatResponse(result.response(), result.provider());
    }
}
