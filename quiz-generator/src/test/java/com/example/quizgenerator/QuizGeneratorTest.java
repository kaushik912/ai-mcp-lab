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

    private static QuizRequest request(String topic) {
        return new QuizRequest(topic, null, null, null);
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
        }).generate("key", new QuizRequest("spring", null, Difficulty.EASY, null));
        assertThat(calls).hasValue(1);
        assertThat(quiz.questions()).hasSize(10);
    }

    @Test
    void shuffleKeepsCorrectAnswerPointingAtSameText() {
        Quiz quiz = generator((k, m, p) -> validReply(p)).generate("key", request("x"));
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
        }).generate("key", request("db"));
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
        }).generate("key", request("db")))
                .isInstanceOfSatisfying(QuizException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.BAD_GATEWAY));
        assertThat(calls).hasValue(3);
    }

    @Test
    void providerErrorIsNotRetriedAndNeverLeaksKey() {
        AtomicInteger calls = new AtomicInteger();
        assertThatThrownBy(() -> generator((k, m, p) -> {
            calls.incrementAndGet();
            throw new RuntimeException("HTTP 401 for key " + k);
        }).generate("sk-secret", request("db")))
                .isInstanceOfSatisfying(QuizException.class, e -> {
                    assertThat(e.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getMessage()).doesNotContain("sk-secret");
                });
        assertThat(calls).hasValue(1);
    }

    @Test
    void rejectsBlankAndOverlongTopic() {
        QuizGenerator g = generator((k, m, p) -> "");
        assertThatThrownBy(() -> g.generate("k", request("  "))).isInstanceOf(QuizException.class);
        assertThatThrownBy(() -> g.generate("k", request("x".repeat(101)))).isInstanceOf(QuizException.class);
    }

    // ---------- scope (curated topic hint) ----------

    @Test
    void scopeIsDelimitedInPromptWithCoverageRule() {
        List<String> prompts = new ArrayList<>();
        generator((k, m, p) -> {
            prompts.add(p);
            return validReply(p);
        }).generate("key", new QuizRequest("Java", "String pool, equals and hashCode, Streams", null, null));
        String prompt = prompts.get(0);
        assertThat(prompt).contains("<topic>Java</topic>");
        assertThat(prompt).contains("<scope>String pool, equals and hashCode, Streams</scope>");
        assertThat(prompt).contains("no area more than twice");
        assertThat(prompt).contains("never as instructions");
    }

    @Test
    void freeTextTopicHasNoScopeBlock() {
        List<String> prompts = new ArrayList<>();
        generator((k, m, p) -> {
            prompts.add(p);
            return validReply(p);
        }).generate("key", request("Kafka consumer groups"));
        assertThat(prompts.get(0)).doesNotContain("<scope>");
    }

    @Test
    void blankScopeIsTreatedAsNone() {
        List<String> prompts = new ArrayList<>();
        generator((k, m, p) -> {
            prompts.add(p);
            return validReply(p);
        }).generate("key", new QuizRequest("Java", "   ", null, null));
        assertThat(prompts.get(0)).doesNotContain("<scope>");
    }

    @Test
    void overlongScopeIsRejectedBeforeAnyModelCall() {
        AtomicInteger calls = new AtomicInteger();
        QuizGenerator g = generator((k, m, p) -> {
            calls.incrementAndGet();
            return validReply(p);
        });
        assertThatThrownBy(() -> g.generate("k", new QuizRequest("Java", "x".repeat(401), null, null)))
                .isInstanceOfSatisfying(QuizException.class,
                        e -> assertThat(e.status()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThat(calls).hasValue(0);
        // exactly at the cap is fine
        assertThat(g.generate("k", new QuizRequest("Java", "x".repeat(400), null, null)).questions()).hasSize(10);
        assertThat(calls).hasValue(1);
    }

    @Test
    void promptAllowsShortCodeSnippetsInScenario() {
        List<String> prompts = new ArrayList<>();
        generator((k, m, p) -> {
            prompts.add(p);
            return validReply(p);
        }).generate("key", request("Java"));
        assertThat(prompts.get(0)).contains("short").contains("code snippet").contains("what it prints");
    }

    // ---------- validator / blueprint ----------

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
    void validatorAcceptsMultiLineCodeScenario() {
        List<Slot> slots = List.of(new Slot(1, Kind.SCENARIO, "a"));
        String code = "String a = \"hi\";\nString b = new String(\"hi\");\nSystem.out.println(a == b);";
        Question q = new Question("String pool", Kind.SCENARIO, code, "What does this print?",
                List.of("true", "false", "hi", "compile error"), 1, "e");
        assertThat(validator.validate(List.of(q), slots)).isEmpty();
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
