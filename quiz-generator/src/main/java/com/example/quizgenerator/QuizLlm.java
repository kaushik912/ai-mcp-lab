package com.example.quizgenerator;

/** Seam around the model call so the retry loop can be tested without a network. */
@FunctionalInterface
public interface QuizLlm {
    /** Returns the raw model text. Must never log or echo {@code apiKey}. */
    String complete(String apiKey, String model, String prompt);
}
