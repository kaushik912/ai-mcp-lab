package com.example.sampleclient.prompt;

public final class PromptTemplates {

    private PromptTemplates() {
    }

    public static String personsByNationality(String nationality) {
        return "Find persons with %s nationality.".formatted(nationality);
    }

    public static String personCountByNationality(String nationality) {
        return "How many persons come from %s?".formatted(nationality);
    }

    public static String accountCountByPersonId(Long personId) {
        return "How many accounts has person with %d ID?".formatted(personId);
    }

    public static String accountBalanceByPersonId(Long personId) {
        return ("How many accounts has person with %d ID? Return person name, "
                + "nationality and a total balance on his/her accounts.").formatted(personId);
    }
}
