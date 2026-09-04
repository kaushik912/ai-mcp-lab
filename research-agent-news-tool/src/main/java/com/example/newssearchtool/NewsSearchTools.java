package com.example.newssearchtool;

import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class NewsSearchTools {

    private final NewsSearchService newsSearchService;

    public NewsSearchTools(NewsSearchService newsSearchService) {
        this.newsSearchService = newsSearchService;
    }

    @Tool(description = "Searches recent news headlines for a topic and returns matching headlines")
    public List<String> searchNews(@ToolParam(description = "topic or keyword to search news for") String topic) {
        return newsSearchService.search(topic);
    }
}
