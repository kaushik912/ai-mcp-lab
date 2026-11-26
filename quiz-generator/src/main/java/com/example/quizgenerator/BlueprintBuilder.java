package com.example.quizgenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Component;

/**
 * Plans the 10 questions on the server so variety does not depend on the model's mood:
 * a random kind mix and 10 distinct angles, shuffled every request.
 */
@Component
public class BlueprintBuilder {

    public static final int QUESTION_COUNT = 10;

    static final List<String> ANGLES = List.of(
            "trade-off between two reasonable approaches",
            "diagnosing a failure or unexpected behaviour",
            "choosing the right design for a requirement",
            "why something works the way it does (internals / reasoning)",
            "spotting a common misconception",
            "performance or efficiency implication",
            "scalability or growth problem",
            "reliability, consistency or data-correctness concern",
            "security or safety concern",
            "tricky edge case or boundary condition",
            "comparison of two similar concepts",
            "what happens in production under load or failure",
            "choosing what NOT to do and why",
            "ordering / sequence of events and why it matters");

    public List<Slot> build(Random random) {
        int scenarios = 3 + random.nextInt(3); // 3..5 scenario questions, rest concept
        List<Kind> kinds = new ArrayList<>();
        for (int i = 0; i < QUESTION_COUNT; i++) {
            kinds.add(i < scenarios ? Kind.SCENARIO : Kind.CONCEPT);
        }
        Collections.shuffle(kinds, random);

        List<String> angles = new ArrayList<>(ANGLES);
        Collections.shuffle(angles, random);

        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < QUESTION_COUNT; i++) {
            slots.add(new Slot(i + 1, kinds.get(i), angles.get(i)));
        }
        return slots;
    }
}
