package com.aicommerce.storefront.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;

/** Offline: no API key needed. Checks chunking and metadata, not embeddings. */
class KnowledgeBaseDocumentsTest {

    private final List<Document> docs = load();

    private static List<Document> load() {
        try {
            return KnowledgeBaseConfig.loadDocuments(
                    new ClassPathResource("docs/return-and-warranty-policy.md"),
                    new ClassPathResource("docs/product-guides.md"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<Document> ofType(String type) {
        return docs.stream().filter(d -> type.equals(d.getMetadata().get("docType"))).toList();
    }

    @Test
    void policyIsSplitIntoOneChunkPerRule() {
        assertThat(ofType("POLICY")).extracting(d -> d.getMetadata().get("sectionId"))
                .containsExactly("POL-1", "POL-2", "POL-3", "POL-4", "POL-5", "POL-6", "POL-7", "POL-8", "POL-9");
    }

    @Test
    void everySeededProductHasACatalogGuide() {
        assertThat(ofType("CATALOG")).extracting(d -> d.getMetadata().get("sectionId"))
                .containsExactlyInAnyOrder("SUMMIT-SHOE-01", "URBAN-RUN-01", "NIMBUS-JKT-01", "THERMO-BASE-01",
                        "TRAIL-HYDRO-01", "AERO-BUDS-01", "VOLT-BANK-01", "PURE-BRUSH-01");
    }

    @Test
    void policyStatesTheNumbersJavaWillEnforce() {
        String grace = text("POL-4");
        assertThat(grace).contains("day 31", "day 45", "80%", "90%");
        assertThat(text("POL-1")).contains("30 days");
        assertThat(text("POL-2")).contains("14-day");
        assertThat(text("POL-7")).contains("24 hours", "₹500");
    }

    private String text(String sectionId) {
        return docs.stream().filter(d -> sectionId.equals(d.getMetadata().get("sectionId")))
                .findFirst().orElseThrow().getText();
    }
}
