package com.example.quizgenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

/** Strict format check. Returns human-readable errors (fed back to the model on retry); empty list = valid. */
@Component
public class QuizValidator {

    static final int MAX_SCENARIO = 900;
    static final int MAX_QUESTION = 350;
    static final int MAX_OPTION = 250;
    static final int MAX_EXPLANATION = 900;

    public List<String> validate(List<Question> questions, List<Slot> slots) {
        List<String> errors = new ArrayList<>();
        if (questions == null) {
            return List.of("\"questions\" array is missing");
        }
        if (questions.size() != slots.size()) {
            errors.add("expected exactly " + slots.size() + " questions but got " + questions.size());
        }
        Set<String> subtopics = new HashSet<>();
        Set<String> texts = new HashSet<>();
        int n = Math.min(questions.size(), slots.size());
        for (int i = 0; i < n; i++) {
            check(i + 1, questions.get(i), slots.get(i), subtopics, texts, errors);
        }
        return errors;
    }

    private void check(int no, Question q, Slot slot, Set<String> subtopics, Set<String> texts, List<String> errors) {
        String p = "Q" + no + ": ";
        if (q == null) {
            errors.add(p + "is null");
            return;
        }
        if (q.kind() != slot.kind()) {
            errors.add(p + "kind must be " + slot.kind() + " but was " + q.kind());
        }
        if (blank(q.subtopic())) {
            errors.add(p + "subtopic is blank");
        } else if (!subtopics.add(norm(q.subtopic()))) {
            errors.add(p + "subtopic \"" + q.subtopic() + "\" repeats an earlier question; every subtopic must be different");
        }
        if (blank(q.question())) {
            errors.add(p + "question is blank");
        } else {
            if (q.question().length() > MAX_QUESTION) {
                errors.add(p + "question longer than " + MAX_QUESTION + " chars");
            }
            if (!texts.add(norm(q.question()))) {
                errors.add(p + "question text duplicates an earlier question");
            }
        }
        if (slot.kind() == Kind.SCENARIO) {
            if (blank(q.scenario())) {
                errors.add(p + "SCENARIO needs a non-blank \"scenario\" paragraph");
            } else if (q.scenario().length() > MAX_SCENARIO) {
                errors.add(p + "scenario longer than " + MAX_SCENARIO + " chars");
            }
        } else if (!blank(q.scenario())) {
            errors.add(p + "CONCEPT must have no \"scenario\" (omit it or use null)");
        }
        checkOptions(p, q, errors);
        if (blank(q.explanation())) {
            errors.add(p + "explanation is blank");
        } else if (q.explanation().length() > MAX_EXPLANATION) {
            errors.add(p + "explanation longer than " + MAX_EXPLANATION + " chars");
        }
    }

    private void checkOptions(String p, Question q, List<String> errors) {
        List<String> opts = q.options();
        if (opts == null || opts.size() != 4) {
            errors.add(p + "options must be exactly 4 strings");
        } else {
            Set<String> seen = new HashSet<>();
            for (String o : opts) {
                if (blank(o)) {
                    errors.add(p + "an option is blank");
                } else {
                    if (o.length() > MAX_OPTION) {
                        errors.add(p + "an option is longer than " + MAX_OPTION + " chars");
                    }
                    if (!seen.add(norm(o))) {
                        errors.add(p + "options must be distinct (duplicate: \"" + o + "\")");
                    }
                }
            }
        }
        if (q.correctIndex() == null || q.correctIndex() < 0 || q.correctIndex() > 3) {
            errors.add(p + "correctIndex must be an integer 0-3");
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String norm(String s) {
        return s.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
