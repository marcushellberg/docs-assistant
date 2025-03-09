package org.vaadin.marcus.docsassistant.advisors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * A {@link CallAroundAdvisor} and {@link StreamAroundAdvisor} that uses a guardrail LLM to
 * determine if a user's question is appropriate to answer based on provided criteria.
 *
 * <p>This advisor takes a simple description of what types of questions are acceptable,
 * then uses a built-in template to evaluate each user question against those criteria.
 */
public class GuardRailAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private static final Logger logger = LoggerFactory.getLogger(GuardRailAdvisor.class);

    private static final String DEFAULT_FAILURE_RESPONSE = "I'm sorry, but your question doesn't match our supported topics. Please ask a question related to our area of expertise.";

    private static final String DEFAULT_GUARDRAIL_TEMPLATE = """
        You are a guardrail system that evaluates if user questions are acceptable based on specific criteria.
        
        ACCEPTABLE QUESTION CRITERIA:
        ${acceptanceCriteria}
        
        USER QUESTION:
        ${question}
        
        EVALUATION INSTRUCTIONS:
        1. Consider ONLY if the question matches the acceptance criteria. Do not answer the question.
        2. Be objective and fair in your evaluation.
        3. Evaluate strictly based on relevance to the criteria, not on how the question is phrased.
        
        First, provide a brief, objective analysis of the question against the criteria.
        Then, make a final determination as either "ACCEPT" or "REJECT".
        
        OUTPUT YOUR DECISION ONLY AS THE FINAL LINE OF YOUR RESPONSE, FORMATTED EXACTLY AS:
        DECISION: [ACCEPT/REJECT]
        """;

    private final ChatClient guardrailClient;
    private final PromptTemplate internalTemplate;
    private final String failureResponse;
    private final int order;

    /**
     * Creates a new GuardRailAdvisor.
     *
     * @param chatClientBuilder  the builder for the chat client to use for guardrail evaluation
     * @param acceptanceCriteria description of what makes a question acceptable
     * @param failureResponse    the response to return if the guardrail check fails
     * @param order              the order of this advisor in the chain
     */
    public GuardRailAdvisor(ChatClient.Builder chatClientBuilder, String acceptanceCriteria,
                            String failureResponse, int order) {
        Assert.notNull(chatClientBuilder, "ChatClient.Builder must not be null!");
        Assert.notNull(acceptanceCriteria, "Acceptance criteria must not be null!");
        Assert.notNull(failureResponse, "Failure response must not be null!");

        this.guardrailClient = chatClientBuilder.build();
        this.internalTemplate = new PromptTemplate(DEFAULT_GUARDRAIL_TEMPLATE,
            Map.of("acceptanceCriteria", acceptanceCriteria));
        this.failureResponse = failureResponse;
        this.order = order;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        String userQuestion = extractLatestUserMessage(advisedRequest);

        if (userQuestion == null || userQuestion.trim().isEmpty()) {
            logger.debug("No user question found, allowing request to proceed");
            return chain.nextAroundCall(advisedRequest);
        }

        boolean isAcceptable = checkAcceptability(userQuestion);

        if (!isAcceptable) {
            logger.debug("Question '{}' failed guardrail check", userQuestion);
            return createFailureResponse(advisedRequest);
        }

        logger.debug("Question '{}' passed guardrail check", userQuestion);
        return chain.nextAroundCall(advisedRequest);
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        String userQuestion = extractLatestUserMessage(advisedRequest);

        if (userQuestion == null || userQuestion.trim().isEmpty()) {
            logger.debug("No user question found, allowing request to proceed");
            return chain.nextAroundStream(advisedRequest);
        }

        boolean isAcceptable = checkAcceptability(userQuestion);

        if (!isAcceptable) {
            logger.debug("Question '{}' failed guardrail check", userQuestion);
            return Flux.just(createFailureResponse(advisedRequest));
        }

        logger.debug("Question '{}' passed guardrail check", userQuestion);
        return chain.nextAroundStream(advisedRequest);
    }

    /**
     * Extracts the latest user message from the request.
     *
     * @param advisedRequest the advised request
     * @return the latest user message, or null if not found
     */
    private String extractLatestUserMessage(AdvisedRequest advisedRequest) {
        return advisedRequest.userText();
    }

    /**
     * Checks if a question is acceptable according to the guardrail.
     *
     * @param question the question to check
     * @return true if the question is acceptable, false otherwise
     */
    private boolean checkAcceptability(String question) {
        try {
            String promptText = internalTemplate.render(Map.of("question", question));

            String response = guardrailClient.prompt()
                .user(promptText)
                .options(ChatOptions.builder().temperature(0.0).build())
                .call()
                .content();

            logger.debug("Guardrail evaluation response: {}", response);

            boolean acceptable = response.contains("DECISION: ACCEPT");
            if (!acceptable && !response.contains("DECISION: REJECT")) {
                logger.warn("Unexpected guardrail response format: {}", response);
                return true; // Default to allowing if the response format is unexpected
            }

            return acceptable;
        } catch (Exception e) {
            logger.error("Error during guardrail check", e);
            return true; // Default to allowing in case of errors
        }
    }

    /**
     * Creates a failure response with the configured failure message.
     *
     * @param advisedRequest the original advised request
     * @return an advised response with the failure message
     */
    private AdvisedResponse createFailureResponse(AdvisedRequest advisedRequest) {
        return new AdvisedResponse(
            ChatResponse.builder()
                .generations(java.util.List.of(new Generation(new AssistantMessage(this.failureResponse))))
                .build(),
            advisedRequest.adviseContext()
        );
    }

    /**
     * Builder for creating GuardRailAdvisor instances.
     */
    public static final class Builder {
        private ChatClient.Builder chatClientBuilder;
        private String acceptanceCriteria;
        private String failureResponse = DEFAULT_FAILURE_RESPONSE;
        private int order = Ordered.HIGHEST_PRECEDENCE;

        private Builder() {
        }

        public Builder chatClientBuilder(ChatClient.Builder chatClientBuilder) {
            this.chatClientBuilder = chatClientBuilder;
            return this;
        }

        public Builder acceptanceCriteria(String acceptanceCriteria) {
            this.acceptanceCriteria = acceptanceCriteria;
            return this;
        }

        public Builder failureResponse(String failureResponse) {
            this.failureResponse = failureResponse;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public GuardRailAdvisor build() {
            Assert.notNull(chatClientBuilder, "ChatClient.Builder must not be null!");
            Assert.notNull(acceptanceCriteria, "Acceptance criteria must not be null!");
            return new GuardRailAdvisor(this.chatClientBuilder, this.acceptanceCriteria,
                this.failureResponse, this.order);
        }
    }
}