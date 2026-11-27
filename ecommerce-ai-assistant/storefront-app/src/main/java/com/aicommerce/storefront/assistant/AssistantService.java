package com.aicommerce.storefront.assistant;

import com.aicommerce.storefront.security.CurrentUserService;
import com.aicommerce.storefront.security.CurrentUserService.AuthenticatedUser;
import com.aicommerce.storefront.tools.ToolIdentity;
import com.aicommerce.storefront.trace.AgentTrace;
import com.aicommerce.storefront.trace.AgentTraceAdvisor;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

/**
 * One chat turn. Identity flows ONLY from the security context into:
 *   - ToolContext (read by local @Tool methods),
 *   - the memory conversation id (username:sessionId, so users never share history),
 *   - the trace (shown as "Server-Injected User").
 * Nothing the user types can change any of these.
 */
@Service
public class AssistantService {

    /** One retrieved chunk, as shown in the X-Ray trace drawer. */
    public record RetrievedChunk(String docType, String sectionId, String title, Double score) {}

    public record AssistantReply(String answer, List<RetrievedChunk> ragChunks, AgentTrace trace) {}

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ToolRegistry tools;
    private final CurrentUserService currentUser;

    public AssistantService(ChatClient assistantChatClient, ChatMemory chatMemory, ToolRegistry tools,
                            CurrentUserService currentUser) {
        this.chatClient = assistantChatClient;
        this.chatMemory = chatMemory;
        this.tools = tools;
        this.currentUser = currentUser;
    }

    public AssistantReply ask(String sessionId, String question) {
        AuthenticatedUser user = currentUser.require();
        ToolRegistry.TurnTools turnTools = tools.forTurn();

        ChatClientResponse response = chatClient.prompt()
                .user(question)
                .toolCallbacks(turnTools.callbacks())
                .toolContext(Map.of(ToolIdentity.USER_ID, user.id()))
                .advisors(a -> a
                        .param(ChatMemory.CONVERSATION_ID, conversationId(user, sessionId))
                        .param(AgentTraceAdvisor.USER_ID_PARAM, user.id())
                        .param(AgentTraceAdvisor.USERNAME_PARAM, user.username()))
                .call()
                .chatClientResponse();

        String answer = response.chatResponse() == null ? ""
                : response.chatResponse().getResult().getOutput().getText();
        AgentTrace trace = (AgentTrace) response.context().get(AgentTraceAdvisor.TRACE_KEY);
        if (turnTools.note() != null) {
            trace.addNote(turnTools.note());
        }
        List<RetrievedChunk> chunks = trace.getRagHits().stream()
                .map(h -> new RetrievedChunk(h.docType(), h.sectionId(), h.title(), h.score()))
                .toList();
        return new AssistantReply(answer, chunks, trace);
    }

    /** Forget this user's conversation in this session (used on user switch / demo reset). */
    public void clearConversation(String sessionId) {
        chatMemory.clear(conversationId(currentUser.require(), sessionId));
    }

    static String conversationId(AuthenticatedUser user, String sessionId) {
        return user.username() + ":" + sessionId;
    }
}
