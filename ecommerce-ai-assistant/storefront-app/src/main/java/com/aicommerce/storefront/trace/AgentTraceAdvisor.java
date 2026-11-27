package com.aicommerce.storefront.trace;

import com.aicommerce.storefront.trace.AgentTrace.RagHit;
import java.util.List;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.core.Ordered;

/**
 * Outermost advisor: opens a trace for the turn, lets every inner advisor (memory, RAG, tool loop) run,
 * then harvests the RAG documents and returns the trace in the response context under TRACE_KEY.
 * The injected identity comes from request params set by the server, never from the model.
 */
public class AgentTraceAdvisor implements CallAdvisor {

    public static final String TRACE_KEY = "agent_trace";
    public static final String USER_ID_PARAM = "trace_user_id";
    public static final String USERNAME_PARAM = "trace_username";

    @Override
    public String getName() {
        return "AgentTraceAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // wraps everything else
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        AgentTrace trace = new AgentTrace((Long) request.context().get(USER_ID_PARAM),
                (String) request.context().get(USERNAME_PARAM));
        long t0 = System.nanoTime();
        TraceRecorder.bind(trace);
        try {
            ChatClientResponse response = chain.nextCall(request);
            collectRag(response, trace);
            trace.setTotalMs((System.nanoTime() - t0) / 1_000_000);
            return response.mutate().context(TRACE_KEY, trace).build();
        } finally {
            TraceRecorder.unbind();
        }
    }

    @SuppressWarnings("unchecked")
    private static void collectRag(ChatClientResponse response, AgentTrace trace) {
        Object docs = response.context().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
        if (docs instanceof List<?> list) {
            for (Document d : (List<Document>) list) {
                trace.addRag(new RagHit(String.valueOf(d.getMetadata().get("docType")),
                        String.valueOf(d.getMetadata().get("sectionId")),
                        String.valueOf(d.getMetadata().get("title")), d.getScore()));
            }
        }
    }
}
