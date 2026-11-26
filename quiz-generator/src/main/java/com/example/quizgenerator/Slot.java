package com.example.quizgenerator;

/** One planned question: its kind and the cognitive angle it must take. */
public record Slot(int number, Kind kind, String angle) {
}
