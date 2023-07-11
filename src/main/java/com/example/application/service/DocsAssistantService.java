package com.example.application.service;

import com.example.application.service.openai.OpenAIService;
import com.example.application.service.openai.model.ChatCompletionMessage;
import com.example.application.service.pinecone.PineconeService;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocsAssistantService {

    private final int MAX_TOKENS = 4096;
    private final int MAX_RESPONSE_TOKENS = 1024;
    private final int MAX_CONTEXT_TOKENS = 1536;

    private final OpenAIService openAIService;
    private final PineconeService pineconeService;
    private final Encoding tokenizer;

    private final List<Framework> supportedFrameworks = List.of(
            new Framework("Flow", "flow"),
            new Framework("Hilla with React", "hilla-react"),
            new Framework("Hilla with Lit", "hilla-lit")
    );

    public DocsAssistantService(OpenAIService openAIService, PineconeService pineconeService) {
        this.openAIService = openAIService;
        this.pineconeService = pineconeService;
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        tokenizer = registry.getEncoding(EncodingType.CL100K_BASE);
    }

    public List<Framework> getSupportedFrameworks() {
        return supportedFrameworks;
    }

    /**
     * Finds similar documents in the documentation and calls the OpenAI chat completion API.
     *
     * @return The completion as a stream of chunks
     */
    public Flux<String> getCompletionStream(List<ChatCompletionMessage> history, String framework) {
        if (history.isEmpty()) {
            return Flux.error(new RuntimeException("History is empty"));
        }

        var question = history.get(history.size() - 1).getContent();

        return openAIService
                .moderate(history)
                .flatMap(isContentSafe -> isContentSafe ?
                        openAIService.createEmbedding(question) :
                        Mono.error(new RuntimeException("Failed to get embedding")))
                .flatMap(embedding -> pineconeService.findSimilarDocuments(embedding, 10, framework))
                .map(similarDocuments -> getPromptWithContext(history, similarDocuments, framework))
                .flatMapMany(openAIService::generateCompletionStream);
    }

    private List<ChatCompletionMessage> getPromptWithContext(List<ChatCompletionMessage> history,
                                                             List<String> contextDocs, String framework) {
        var contextString = getContextString(contextDocs);
        var systemMessages = new ArrayList<ChatCompletionMessage>();
        var fullFramework = supportedFrameworks.stream()
                .filter(f -> f.getValue().equals(framework))
                .findFirst()
                .orElseThrow()
                .getLabel();

        systemMessages.add(new ChatCompletionMessage(
                ChatCompletionMessage.Role.SYSTEM,
                String.format("You are a senior Vaadin expert. You love to help developers! Answer the user's question about %s development with the help of the information in the provided documentation.", fullFramework)
        ));
        systemMessages.add(new ChatCompletionMessage(
                ChatCompletionMessage.Role.USER,
                String.format(
                        """
                                Here is the documentation:
                                ===
                                %s
                                ===
                                """, contextString)
        ));
        systemMessages.add(new ChatCompletionMessage(
                ChatCompletionMessage.Role.USER,
                """
                        You must also follow the below rules when answering:
                        - Prefer splitting your response into multiple paragraphs
                        - Output as markdown
                        - Always include code snippets if available
                        """
        ));


        return capMessages(systemMessages, history);
    }

    /**
     * Returns a string of up to MAX_CONTEXT_TOKENS tokens from the contextDocs
     *
     * @param contextDocs The context documents
     */
    private String getContextString(List<String> contextDocs) {
        var tokenCount = 0;
        var stringBuilder = new StringBuilder();
        for (var doc : contextDocs) {
            tokenCount += tokenizer.encode(doc).size() + 2; // +2 for the newline and the --- separator
            if (tokenCount > MAX_CONTEXT_TOKENS) {
                break;
            }
            stringBuilder.append(doc);
            stringBuilder.append("\n---\n");
        }

        return stringBuilder.toString();
    }

    /**
     * Removes old messages from the history until the total number of tokens + MAX_RESPONSE_TOKENS stays under MAX_TOKENS
     *
     * @param systemMessages The system messages including context and prompt
     * @param history        The history of messages. The last message is the user question, do not remove it.
     * @return The capped messages that can be sent to the OpenAI API.
     */
    private List<ChatCompletionMessage> capMessages(List<ChatCompletionMessage> systemMessages,
                                                    List<ChatCompletionMessage> history) {
        var availableTokens = MAX_TOKENS - MAX_RESPONSE_TOKENS;
        var cappedHistory = new ArrayList<>(history);

        var tokens = getTokenCount(systemMessages) + getTokenCount(cappedHistory);

        while (tokens > availableTokens) {
            if (cappedHistory.size() == 1) {
                throw new RuntimeException("Cannot cap messages further, only user question left");
            }

            cappedHistory.remove(0);
            tokens = getTokenCount(systemMessages) + getTokenCount(cappedHistory);
        }

        var cappedMessages = new ArrayList<>(systemMessages);
        cappedMessages.addAll(cappedHistory);

        return cappedMessages;
    }


    /**
     * Returns the number of tokens in the messages.
     * See https://github.com/openai/openai-cookbook/blob/834181d5739740eb8380096dac7056c925578d9a/examples/How_to_count_tokens_with_tiktoken.ipynb
     *
     * @param messages The messages to count the tokens of
     * @return The number of tokens in the messages
     */
    private int getTokenCount(List<ChatCompletionMessage> messages) {
        var tokenCount = 3; // every reply is primed with <|start|>assistant<|message|>
        for (var message : messages) {
            tokenCount += getMessageTokenCount(message);
        }
        return tokenCount;
    }

    /**
     * Returns the number of tokens in the message.
     *
     * @param message The message to count the tokens of
     * @return The number of tokens in the message
     */
    private int getMessageTokenCount(ChatCompletionMessage message) {
        var tokens = 4; // every message follows <|start|>{role/name}\n{content}<|end|>\n

        tokens += tokenizer.encode(message.getRole().toString()).size();
        tokens += tokenizer.encode(message.getContent()).size();

        return tokens;
    }
}
