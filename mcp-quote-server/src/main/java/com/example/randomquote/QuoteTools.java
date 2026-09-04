package com.example.randomquote;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class QuoteTools {

    private final QuoteService quoteService; // your existing service

    public QuoteTools(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @Tool(description = "Returns a random inspirational quote")
    public String randomQuote() {
        return quoteService.getRandomQuote();
    }
}