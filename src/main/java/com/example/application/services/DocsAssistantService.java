package com.example.application.services;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@BrowserCallable
@AnonymousAllowed
public class DocsAssistantService {

    private final ChatClient flowChatClient;
    private final ChatClient hillaChatClient;

    private final String SYSTEM_PROMPT = """
                You are Koda, an AI assistant specialized in Vaadin development. Your primary goal is to assist users with their questions related to {framework} development. Your responses should be helpful, provide relevant code snippets, and be based on the included documentation and other trusted sources. Follow these guidelines:
                
                1. **Clarity and Precision**: Ensure your answers are clear, concise, and directly address the user's query.
                2. **Clear Steps**: When applicable, provide clear steps or instructions to help users understand the solution.
                3. **Code Snippets**: When applicable, include relevant and well-commented code snippets to illustrate your points.
                4. **Documentation**: Base your answers on the included Vaadin documentation. Always provide references or links to the documentation where necessary.
                5. **User Guidance**: Guide users through troubleshooting steps or best practices when they face issues.
                6. **Politeness**: Maintain a polite and professional tone in all interactions.
                """;
    private final ChatMemory chatMemory;


    public DocsAssistantService(
            ChatClient.Builder modelBuilder,
            @Qualifier("flow") VectorStore flowVectorStore,
            @Qualifier("hilla") VectorStore hillaVectorStore,
            ChatMemory chatMemory
    ) {
        this.chatMemory = chatMemory;
        flowChatClient = modelBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new PromptChatMemoryAdvisor(chatMemory),
                        new QuestionAnswerAdvisor(flowVectorStore, SearchRequest.defaults()))
                .build();

        hillaChatClient = modelBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new PromptChatMemoryAdvisor(chatMemory),
                        new QuestionAnswerAdvisor(hillaVectorStore, SearchRequest.defaults()))
                .build();
    }


    public Flux<String> chat(String chatId, String userMessage, String framework) {
        ChatClient chatClient = "flow".equalsIgnoreCase(framework) ? flowChatClient : hillaChatClient;

        return chatClient.prompt()
                .system(s -> s.param("framework", framework))
                .user(userMessage)
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
                .stream()
                .content();
    }

    public void clearChatMemory(String chatId) {
        chatMemory.clear(chatId);
    }
}
