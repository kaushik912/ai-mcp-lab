package com.example.quizgenerator;

import java.util.List;

public record Quiz(String topic, List<Question> questions) {
}
