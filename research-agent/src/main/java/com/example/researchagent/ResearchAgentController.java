package com.example.researchagent;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent")
public class ResearchAgentController {

    private final ChatClient researchAgent;
    private final VectorStore vectorStore;

    public ResearchAgentController(ChatClient researchAgent, VectorStore vectorStore) {
        this.researchAgent = researchAgent;
        this.vectorStore = vectorStore;
    }

    /** Seeds the vector store with context documents for later retrieval. */
    @PostMapping("/knowledge")
    public String addKnowledge(@RequestBody List<String> documents) {
        vectorStore.add(documents.stream().map(Document::new).toList());
        return "ingested " + documents.size() + " document(s)";
    }

    /**
     * Queries the agent. Same sessionId across calls keeps conversation
     * memory; the model decides on its own whether to call searchNews or
     * pull from the vector store.
     */
    @GetMapping("/ask")
    public String ask(@RequestParam String question,
                       @RequestParam(defaultValue = "default") String sessionId) {
        return researchAgent.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .user(question)
                .call()
                .content();
    }
}
