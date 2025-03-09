package org.vaadin.marcus.docsassistant.client;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import jakarta.annotation.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.web.multipart.MultipartFile;
import org.vaadin.marcus.docsassistant.advisors.GuardRailAdvisor;
import org.vaadin.marcus.docsassistant.chat.ChatService;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@BrowserCallable
@AnonymousAllowed
public class DocsAssistantService implements ChatService<DocsAssistantService.ChatOptions> {

    // Object for passing additional options from the client
    public record ChatOptions(String framework) {
    }

    private static final String SYSTEM_MESSAGE = """
        You are Koda, an AI assistant specialized in Vaadin development.
        Answer the user's questions regarding the {framework} framework.
        Your primary goal is to assist users with their questions related to Vaadin development.
        Your responses should be helpful, clear, succinct, and provide relevant code snippets.
        Avoid making the user feel dumb by using phrases like "straightforward", "easy", "simple", "obvious", etc.
        Refer to the provided documents for up-to-date information and best practices.
        You may use Mermaid diagrams to visualize concepts if you deem it useful.
        """;

    private static final String GUARDRAIL_ACCEPTANCE_CRITERIA = """
        Questions should be related to one or more of the following topics:
        1. Vaadin framework and its components
        2. Java development, including core Java, Java EE, or Spring Framework
        3. Web development with Java-based frameworks
        4. Frontend technologies commonly used with Java backends, such as React.
        
        Questions about unrelated programming languages, non-technical topics,
        or topics clearly outside of Java web development are NOT acceptable.
        """;

    private static final String GUARDRAIL_FAILURE_RESPONSE = "I'm sorry, but your question doesn't appear to be related to Vaadin, " +
        "Java development, or web development with Java frameworks. Could you please ask a question " +
        "related to these topics?";

    private static final String CONTEXT_PROMPT = """
        Context information is below.
        
        ---------------------
        {context}
        ---------------------
        
        Answer the user's question, using the provided information as context when necessary.
        Avoid statements like "Based on the context..." or "The provided information...".
        
        Query: {query}
        
        Answer:
        """;

    private static final String NO_CONTEXT_PROMPT = """
        The user query is not directly covered in the documentation.
        Do your best to answer the user's question without context, letting them know if you are not sure.
        """;

    private final ChatClient chatClient;
    private final ChatClient.Builder builder;
    private final VectorStore vectorStore;
    private final ChatMemory chatMemory;

    public DocsAssistantService(
        ChatClient.Builder builder,
        VectorStore vectorStore,
        ChatMemory chatMemory) {
        this.builder = builder;
        this.vectorStore = vectorStore;
        this.chatMemory = chatMemory;

        chatClient = builder
            .defaultSystem(SYSTEM_MESSAGE)
            .defaultAdvisors(
                new MessageChatMemoryAdvisor(chatMemory),
                GuardRailAdvisor.builder()
                    .chatClientBuilder(builder.build().mutate())
                    .acceptanceCriteria(GUARDRAIL_ACCEPTANCE_CRITERIA)
                    .failureResponse(GUARDRAIL_FAILURE_RESPONSE)
                    .build()
            )
            .build();
    }

    @Override
    public Flux<String> stream(String chatId, String userMessage, @Nullable ChatOptions chatOptions) {
        String framework = chatOptions != null ? chatOptions.framework() : "";

        return chatClient.prompt()
            .system(s -> s.param("framework", framework))
            .user(userMessage)
            .advisors(a -> {
                a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId);
                a.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 20);
            })
            .advisors(RetrievalAugmentationAdvisor.builder()
                .queryTransformers(
                    CompressionQueryTransformer.builder()
                        .chatClientBuilder(builder.build().mutate())
                        .build(),
                    RewriteQueryTransformer.builder()
                        .chatClientBuilder(builder.build().mutate())
                        .build()
                )
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                    .vectorStore(vectorStore)
                    .similarityThreshold(0.6)
                    .topK(10) // TODO: we should add a rerank step when that's supported.
                    .filterExpression(new FilterExpressionBuilder()
                        // Always include the given framework and an empty string to also include general docs
                        .in("framework", framework, "")
                        .build())
                    .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                    .allowEmptyContext(true)
                    .promptTemplate(new PromptTemplate(CONTEXT_PROMPT))
                    .emptyContextPromptTemplate(new PromptTemplate(NO_CONTEXT_PROMPT))
                    .build())
                .build())
            .stream()
            .content();
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
