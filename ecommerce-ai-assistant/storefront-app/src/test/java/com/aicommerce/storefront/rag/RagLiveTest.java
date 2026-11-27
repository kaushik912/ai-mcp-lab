package com.aicommerce.storefront.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.aicommerce.storefront.assistant.AssistantService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * Live: real OpenRouter embeddings + chat. Runs only when OPENROUTER_API_KEY is set.
 * Retrieval asserts are strict (embeddings are deterministic enough);
 * the answer assert is loose (model wording varies).
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".+")
class RagLiveTest {

    @Autowired VectorStore vectorStore;
    @Autowired AssistantService assistant;

    private List<String> topSections(String query, int k) {
        return vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(k).build())
                .stream().map(d -> (String) d.getMetadata().get("sectionId")).toList();
    }

    @Test
    void gracePolicyRetrievedForLateApparelReturn() {
        assertThat(topSections("I bought a jacket 36 days ago, can I still return it?", 3)).contains("POL-4");
    }

    @Test
    void hygieneSealRuleRetrievedForOpenedEarbuds() {
        assertThat(topSections("Can I return earbuds after opening the box?", 3)).contains("POL-3");
    }

    @Test
    void lateDeliveryRuleRetrieved() {
        assertThat(topSections("My order is delayed by three days, do I get any compensation?", 3)).contains("POL-7");
    }

    @Test
    void muddyMonsoonQueryFindsWaterproofTrailShoe() {
        List<Document> hits = vectorStore.similaritySearch(
                SearchRequest.builder().query("Which shoes are best for muddy monsoon trails?").topK(2).build());
        assertThat(hits.get(0).getMetadata().get("sectionId")).isEqualTo("SUMMIT-SHOE-01");
        assertThat(hits.get(0).getScore()).isNotNull();
    }

    @Test
    @WithMockUser(username = "alex")
    void assistantAnswerIsGroundedInGracePolicy() {
        var reply = assistant.ask("rag-test", "What is the policy for returning a jacket 36 days after delivery for a VIP member?");
        System.out.println("ANSWER: " + reply.answer());
        System.out.println("CHUNKS: " + reply.ragChunks());
        assertThat(reply.ragChunks()).extracting(AssistantService.RetrievedChunk::sectionId).contains("POL-4");
        assertThat(reply.answer()).containsAnyOf("90%", "90 %", "voucher", "store credit");
    }
}
