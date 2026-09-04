package com.example.randomquote;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class QuoteService {

    private static final List<String> QUOTES = List.of(
            "The only way to do great work is to love what you do. — Steve Jobs",
            "Simplicity is the soul of efficiency. — Austin Freeman",
            "Programs must be written for people to read. — Harold Abelson",
            "First, solve the problem. Then, write the code. — John Johnson",
            "Talk is cheap. Show me the code. — Linus Torvalds"
    );

    public String getRandomQuote() {
        int index = ThreadLocalRandom.current().nextInt(QUOTES.size());
        return QUOTES.get(index);
    }
}