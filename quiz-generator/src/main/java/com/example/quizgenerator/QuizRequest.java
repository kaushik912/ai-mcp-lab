package com.example.quizgenerator;

/** {@code scope} is an optional hint (areas the topic covers); the UI sends it for the curated topics. */
public record QuizRequest(String topic, String scope, Difficulty difficulty, String model) {
}
