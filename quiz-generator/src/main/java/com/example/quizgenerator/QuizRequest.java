package com.example.quizgenerator;

public record QuizRequest(String topic, Difficulty difficulty, String model) {
}
