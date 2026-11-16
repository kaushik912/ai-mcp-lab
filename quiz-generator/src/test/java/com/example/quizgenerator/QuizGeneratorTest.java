package com.example.quizgenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import tools.jackson.databind.json.JsonMapper;

class QuizGeneratorTest {

    private final BlueprintBuilder blueprint = new BlueprintBuilder();
    private final QuizValidator validator = new QuizValidator();
    private final JsonMapper json = JsonMapper.builder().build();

    private QuizGenerator generator(QuizLlm llm) {
        return new QuizGenerator(llm, blueprint, new QuizPromptBuilder(), validator, json, 3, "m");
    }

    /** Builds a reply matching the blueprint that the generator will compute: we parse kinds from the prompt. */
    private static String validReply(String prompt) {
        List<String> kinds = new ArrayList<>();
        for (String line : prompt.split("\n")) {
            if (line.startsWith("- Q") && line.contains("kind=")) {
                kinds.add(line.substring(line.indexOf("kind=") + 5, line.indexOf(',')));
            }
        }
        StringBuilder sb = new StringBuilder("```json\n{\"questions\":[");
        for (int i = 0; i < kinds.size(); i++) {
            if (i > 0) sb.append(',');
            String scenario = kinds.get(i).equals("SCENARIO") ? "\"Some scenario " + i + "\"" : "null";
            sb.append("{\"subtopic\":\"sub").append(i).append("\",\"kind\":\"").append(kinds.get(i))
                    .append("\",\"scenario\":").append(scenario)
                    .append(",\"question\":\"Question ").append(i).append("?\",")
                    .append("\"options\":[\"a").append(i).append("\",\"b").append(i).append("\",\"c").append(i)
                    .append("\",\"d").append(i).append("\"],\"correctIndex\":1,\"explanation\":\"because ")
                    .append(i).append("\"}");
        }
        return sb.append("]}\n```").toString();
    }

    @Test
    void returnsQuizOnFirstValidReply() {
        AtomicInteger calls = new AtomicInteger();
        Quiz quiz = generator((k, m, p) -> {
            calls.incrementAndGet();
            return validReply(p);
        }).generate("key", new QuizRequest("spring", Difficulty.EASY, null));
        assertThat(calls).hasValue(1);
        assertThat(quiz.questions()).hasSize(10);
    }

    @Test
    void shuffleKeepsCorrectAnswerPointingAtSameText() {
        Quiz quiz = generator((k, m, p) -> validReply(p)).generate("key", new QuizRequest("x", null, null));
        for (Question q : quiz.questions()) {
            String i = q.question().replaceAll("\\D", "");
            assertThat(q.options().get(q.correctIndex())).isEqualTo("b" + i);
        }
    }

    @Test
    void retriesWithErrorFeedbackThenSucceeds() {
        AtomicInteger calls = new AtomicInteger();
        List<String> prompts = new ArrayList<>();
        Quiz quiz = generator((k, m, p) -> {
            prompts.add(p);
            return calls.incrementAndGet() < 3 ? "not json at all" : validReply(p);
        }).generate("key", new QuizRequest("db", null, null));
        assertThat(calls).hasValue(3);
        assertThat(prompts.get(0)).doesNotContain("PREVIOUS REPLY WAS REJECTED");
        assertThat(prompts.get(1)).contains("PREVIOUS REPLY WAS REJECTED");
        assertThat(quiz.questions()).hasSize(10);
    }

    @Test
    void failsWith502AfterThreeBadAttempts() {
        AtomicInteger calls = new AtomicInteger();
        assertThatThrownBy(() -> generator((k, m, p) -> {
            calls.incrementAndGet();
            return "{\"questions\":[]}";
        }).generate("key", new QuizRequest("db", null, null)))
                .isInstanceOfSatisfying(QuizException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.BAD_GATEWAY));
        assertThat(calls).hasValue(3);
    }

    @Test
    void providerErrorIsNotRetriedAndNeverLeaksKey() {
        AtomicInteger calls = new AtomicInteger();
        assertThatThrownBy(() -> generator((k, m, p) -> {
            calls.incrementAndGet();
            throw new RuntimeException("HTTP 401 for key " + k);
        }).generate("sk-secret", new QuizRequest("db", null, null)))
                .isInstanceOfSatisfying(QuizException.class, e -> {
                    assertThat(e.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getMessage()).doesNotContain("sk-secret");
                });
        assertThat(calls).hasValue(1);
    }

    @Test
    void rejectsBlankAndOverlongTopic() {
        QuizGenerator g = generator((k, m, p) -> "");
        assertThatThrownBy(() -> g.generate("k", new QuizRequest("  ", null, null))).isInstanceOf(QuizException.class);
        assertThatThrownBy(() -> g.generate("k", new QuizRequest("x".repeat(101), null, null))).isInstanceOf(QuizException.class);
    }

    @Test
    void validatorCatchesDuplicateSubtopicAndBadIndex() {
        List<Slot> slots = List.of(new Slot(1, Kind.CONCEPT, "a"), new Slot(2, Kind.CONCEPT, "b"));
        Question a = new Question("Same", Kind.CONCEPT, null, "Q one?", List.of("1", "2", "3", "4"), 0, "e");
        Question b = new Question("same", Kind.CONCEPT, null, "Q two?", List.of("1", "2", "3", "4"), 7, "e");
        List<String> errors = validator.validate(List.of(a, b), slots);
        assertThat(errors).anyMatch(e -> e.contains("subtopic"));
        assertThat(errors).anyMatch(e -> e.contains("correctIndex"));
    }

    @Test
    void blueprintHasTenSlotsWithDistinctAngles() {
        List<Slot> slots = blueprint.build(new Random(1));
        assertThat(slots).hasSize(10);
        assertThat(slots.stream().map(Slot::angle).distinct()).hasSize(10);
        long scenarios = slots.stream().filter(s -> s.kind() == Kind.SCENARIO).count();
        assertThat(scenarios).isBetween(3L, 5L);
    }
}
