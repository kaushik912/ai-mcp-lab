package com.example.quizgenerator;

public enum Difficulty {
    EASY("fundamentals; a developer with 0-1 years of experience should manage"),
    MEDIUM("working knowledge; a developer with 2-4 years of experience should manage"),
    HARD("deep understanding, subtle trade-offs and edge cases; senior-level");

    private final String guidance;

    Difficulty(String guidance) { this.guidance = guidance; }

    public String guidance() { return guidance; }
}
