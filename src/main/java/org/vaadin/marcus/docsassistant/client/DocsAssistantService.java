package org.vaadin.marcus.docsassistant.client;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import jakarta.annotation.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.web.multipart.MultipartFile;
import org.vaadin.components.experimental.chat.AiChatService;
import org.vaadin.marcus.docsassistant.advisors.GuardRailAdvisor;
import reactor.core.publisher.Flux;

import java.util.List;

@BrowserCallable
@AnonymousAllowed
public class DocsAssistantService implements AiChatService<DocsAssistantService.ChatOptions> {

    // Object for passing additional options from the client
    public record ChatOptions(String framework) {
    }

    // ThreadLocal to store the current framework option
    private final ThreadLocal<String> currentFramework = new ThreadLocal<>();

    private static final String SYSTEM_MESSAGE = """
        You are Koda, an AI assistant specialized in Vaadin development.
        Your primary goal is to assist users with their questions related to Vaadin development.
        Your responses should be helpful, clear, succinct, and provide relevant code snippets.
        Avoid making the user feel dumb by using phrases like "straightforward", "easy", "simple", "obvious", etc.
        Refer to the provided documents for up-to-date information and best practices.
        You may use Mermaid diagrams to visualize concepts if you deem it useful.
        """;

    private static final String ACCEPTANCE_CRITERIA = """
        Questions should be related to one or more of the following topics:
        1. Vaadin framework and its components
        2. Java development, including core Java, Java EE, or Spring Framework
        3. Web development with Java-based frameworks
        4. Frontend technologies commonly used with Java backends
        
        Questions about unrelated programming languages, non-technical topics,
        or topics clearly outside of Java web development are NOT acceptable.
        """;

    private static final String FAILURE_RESPONSE = "I'm sorry, but your question doesn't appear to be related to Vaadin, " +
        "Java development, or web development with Java frameworks. Could you please ask a question " +
        "related to these topics?";

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public DocsAssistantService(
        ChatClient.Builder builder,
        VectorStore vectorStore,
        ChatMemory chatMemory) {
        this.chatMemory = chatMemory;

        chatClient = builder
            .defaultSystem(SYSTEM_MESSAGE)
            .defaultAdvisors(
                new MessageChatMemoryAdvisor(chatMemory),
                GuardRailAdvisor.builder()
                    .chatClientBuilder(builder.build().mutate())
                    .acceptanceCriteria(ACCEPTANCE_CRITERIA)
                    .failureResponse(FAILURE_RESPONSE)
                    .build(),
                RetrievalAugmentationAdvisor.builder()
                    .queryTransformers(
                        CompressionQueryTransformer.builder()
                            .chatClientBuilder(builder.build().mutate())
                            .build()//,
//                        RewriteQueryTransformer.builder()
//                            .chatClientBuilder(builder.build().mutate())
//                            .build()
                    )
                    .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .similarityThreshold(0.5)
                        .topK(5)
                        .filterExpression(() -> {
                            String framework = currentFramework.get();
                            return new FilterExpressionBuilder()
                                .in("framework", framework != null ? framework : "", "")
                                .build();
                        })
                        .build())
                    .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                    .build()
            )
            .build();
    }

    @Override
    public Flux<String> stream(String chatId, String userMessage, @Nullable ChatOptions chatOptions) {
        String framework = chatOptions != null ? chatOptions.framework() : "";

        // Store the framework in ThreadLocal for use in the filter expression defined in the Supplier above
        currentFramework.set(framework);

        return chatClient.prompt()
            .user(userMessage)
            .stream()
            .content()
            .doFinally(signal -> currentFramework.remove());
    }

    @Override
    public List<Message> getHistory(String chatId) {
        return List.of();
    }

    @Override
    public void closeChat(String chatId) {
        chatMemory.clear(chatId);
    }

    // Attachments are not yet used
    @Override
    public String uploadAttachment(String chatId, MultipartFile multipartFile) {
        return "";
    }

    @Override
    public void removeAttachment(String chatId, String attachmentId) {

    }
}
