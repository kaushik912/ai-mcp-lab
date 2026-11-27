package com.aicommerce.storefront.rag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;

/**
 * Splits a Markdown file into one Document per "## " section.
 *
 * Why not a token splitter: each section here is one self-contained rule or product,
 * so a section is the natural retrieval unit. A chunk never mixes two policies,
 * and the trace drawer can show a readable title ("POL-4 31-45 day grace policy").
 */
public final class MarkdownSectionSplitter {

    private MarkdownSectionSplitter() {}

    /**
     * @param markdown file contents
     * @param docType  metadata tag, e.g. POLICY or CATALOG
     * @param source   file name, kept for citations
     */
    public static List<Document> split(String markdown, String docType, String source) {
        List<Document> docs = new ArrayList<>();
        String title = null;
        StringBuilder body = new StringBuilder();
        for (String line : markdown.split("\\R")) {
            if (line.startsWith("## ")) {
                add(docs, title, body, docType, source);
                title = line.substring(3).trim();
                body.setLength(0);
            } else if (title != null) {
                body.append(line).append('\n');
            }
            // text before the first "## " (file title/intro) is skipped
        }
        add(docs, title, body, docType, source);
        return docs;
    }

    private static void add(List<Document> docs, String title, StringBuilder body, String docType, String source) {
        if (title == null || body.toString().isBlank()) {
            return;
        }
        Map<String, Object> meta = new HashMap<>();
        meta.put("docType", docType);
        meta.put("source", source);
        meta.put("title", title);
        meta.put("sectionId", title.split("\\s+")[0]); // POL-4 or SKU
        // Title is embedded with the body so a query like "jacket" also matches on the heading.
        docs.add(new Document(title + "\n" + body.toString().trim(), meta));
    }
}
