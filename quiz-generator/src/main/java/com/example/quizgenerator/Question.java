package com.example.quizgenerator;

import java.util.List;

/** One MCQ. Fields are nullable only so a malformed model reply can be parsed and then rejected by the validator. */
public record Question(
        String subtopic,
        Kind kind,
        String scenario,
        String question,
        List<String> options,
        Integer correctIndex,
        String explanation) {
}
