package com.example.sampleclient.prompt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplatesTest {

    @Test
    void personsByNationality() {
        assertThat(PromptTemplates.personsByNationality("Polish"))
                .isEqualTo("Find persons with Polish nationality.");
    }

    @Test
    void personCountByNationality() {
        assertThat(PromptTemplates.personCountByNationality("Polish"))
                .isEqualTo("How many persons come from Polish?");
    }

    @Test
    void accountCountByPersonId() {
        assertThat(PromptTemplates.accountCountByPersonId(1L))
                .isEqualTo("How many accounts has person with 1 ID?");
    }

    @Test
    void accountBalanceByPersonId() {
        assertThat(PromptTemplates.accountBalanceByPersonId(1L))
                .isEqualTo("How many accounts has person with 1 ID? Return person name, "
                        + "nationality and a total balance on his/her accounts.");
    }
}
