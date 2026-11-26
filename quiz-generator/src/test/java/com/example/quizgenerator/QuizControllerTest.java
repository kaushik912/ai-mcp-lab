package com.example.quizgenerator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Boots the whole app with NO API key configured (BYOK) and checks the HTTP contract. */
@SpringBootTest
class QuizControllerTest {

    @Autowired
    WebApplicationContext ctx;

    @Test
    void missingKeyHeaderIs401() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(ctx).build();
        mvc.perform(post("/api/quiz").contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"spring\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void blankTopicIs400() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(ctx).build();
        mvc.perform(post("/api/quiz").header("X-OpenRouter-Key", "k").contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\" \"}"))
                .andExpect(status().isBadRequest());
    }
}
