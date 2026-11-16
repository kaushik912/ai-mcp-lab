package com.example.quizgenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * Plan -> prompt -> call model -> parse -> strict validate. On a bad format, re-prompt with the
 * validation errors, up to {@code quiz.max-attempts} times. Errors from the model call itself
 * (bad key, no credits, network) are not retried.
 */
@Service
public class QuizGenerator {

    private static final Logger log = LoggerFactory.getLogger(QuizGenerator.class);

    private final QuizLlm llm;
    private final BlueprintBuilder blueprint;
    private final QuizPromptBuilder promptBuilder;
    private final QuizValidator validator;
    private final JsonMapper json;
    private final int maxAttempts;
    private final String defaultModel;

    public QuizGenerator(QuizLlm llm, BlueprintBuilder blueprint, QuizPromptBuilder promptBuilder,
            QuizValidator validator, JsonMapper json,
            @Value("${quiz.max-attempts}") int maxAttempts,
            @Value("${quiz.model}") String defaultModel) {
        this.llm = llm;
        this.blueprint = blueprint;
        this.promptBuilder = promptBuilder;
        this.validator = validator;
        this.json = json;
        this.maxAttempts = maxAttempts;
        this.defaultModel = defaultModel;
    }

    public Quiz generate(String apiKey, QuizRequest request) {
        String topic = request.topic() == null ? "" : request.topic().strip();
        if (topic.isEmpty() || topic.length() > 100) {
            throw new QuizException(HttpStatus.BAD_REQUEST, "topic is required (max 100 characters)");
        }
        Difficulty difficulty = request.difficulty() == null ? Difficulty.MEDIUM : request.difficulty();
        String model = request.model() == null || request.model().isBlank() ? defaultModel : request.model().strip();

        List<Slot> slots = blueprint.build(ThreadLocalRandom.current());
        List<String> errors = List.of();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String prompt = promptBuilder.build(topic, difficulty, slots, errors);
            String raw = call(apiKey, model, prompt);
            errors = check(raw, slots);
            if (errors.isEmpty()) {
                return new Quiz(topic, shuffled(parse(raw).questions()));
            }
            log.info("quiz attempt {}/{} rejected: {}", attempt, maxAttempts, errors);
        }
        throw new QuizException(HttpStatus.BAD_GATEWAY,
                "model did not return a valid quiz after " + maxAttempts + " attempts; last problems: "
                        + String.join("; ", errors.stream().limit(5).toList()));
    }

    private String call(String apiKey, String model, String prompt) {
        try {
            String raw = llm.complete(apiKey, model, prompt);
            return raw == null ? "" : raw;
        } catch (QuizException e) {
            throw e;
        } catch (RuntimeException e) {
            throw map(e);
        }
    }

    /** Map provider failures to a clear status without leaking details (or the key). */
    static QuizException map(RuntimeException e) {
        String m = String.valueOf(e.getMessage());
        if (m.contains("401")) {
            return new QuizException(HttpStatus.UNAUTHORIZED, "OpenRouter rejected the API key");
        }
        if (m.contains("402")) {
            return new QuizException(HttpStatus.PAYMENT_REQUIRED, "OpenRouter says the key is out of credits");
        }
        if (m.contains("429")) {
            return new QuizException(HttpStatus.TOO_MANY_REQUESTS, "OpenRouter rate limit hit; try again shortly");
        }
        return new QuizException(HttpStatus.BAD_GATEWAY, "model call failed (" + e.getClass().getSimpleName() + ")");
    }

    private List<String> check(String raw, List<Slot> slots) {
        try {
            return validator.validate(parse(raw).questions(), slots);
        } catch (RuntimeException e) {
            return List.of("reply was not valid JSON in the required shape (" + e.getClass().getSimpleName()
                    + "); reply with only the JSON object");
        }
    }

    record Reply(List<Question> questions) {
    }

    private Reply parse(String raw) {
        return json.readValue(stripFences(raw), Reply.class);
    }

    static String stripFences(String raw) {
        String s = raw.strip();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            s = nl < 0 ? "" : s.substring(nl + 1);
            int end = s.lastIndexOf("```");
            if (end >= 0) {
                s = s.substring(0, end);
            }
        }
        int a = s.indexOf('{');
        int b = s.lastIndexOf('}');
        return a >= 0 && b > a ? s.substring(a, b + 1) : s;
    }

    /** Models favour B/C as the right answer; shuffle options and remap correctIndex. */
    static List<Question> shuffled(List<Question> questions) {
        Random random = ThreadLocalRandom.current();
        List<Question> out = new ArrayList<>();
        for (Question q : questions) {
            List<String> options = new ArrayList<>(q.options());
            String correct = options.get(q.correctIndex());
            Collections.shuffle(options, random);
            String scenario = q.scenario() == null || q.scenario().isBlank() ? null : q.scenario();
            out.add(new Question(q.subtopic(), q.kind(), scenario, q.question(), options,
                    options.indexOf(correct), q.explanation()));
        }
        return out;
    }
}
