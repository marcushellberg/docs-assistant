package com.example.application.service.pinecone;

import com.example.application.service.openai.OpenAIService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class PineconeService {

    private final Logger logger = LoggerFactory.getLogger(PineconeService.class);

    private WebClient webClient;

    @Value("${pinecone.api.key}")
    private String PINECONE_API_KEY;

    @Value("${pinecone.api.url}")
    private String PINECONE_API_URL;

    @PostConstruct
    void init() {
        this.webClient = WebClient.builder()
                .baseUrl(PINECONE_API_URL)
                .defaultHeader("Api-Key", PINECONE_API_KEY)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @RegisterReflectionForBinding({QueryResponse.class})
    public Mono<List<String>> findSimilarDocuments(List<Double> embedding, int maxResults, String namespace) {
        logger.debug("Finding similar documents in namespace: {}", namespace);

        var body = Map.of(
                "vector", embedding,
                "topK", maxResults,
                "includeMetadata", true,
                "namespace", namespace
        );

        return this.webClient.post()
                .uri("/query")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(QueryResponse.class)
                .map(QueryResponse::getMatches)
                .flatMapIterable(matches -> matches)
                .map(match -> {
                    if (match.getMetadata() != null && match.getMetadata().containsKey("text")) {
                        return match.getMetadata().get("text");
                    } else {
                        return "";
                    }
                })
                .filter(doc -> !doc.isBlank())
                .collectList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class QueryResponse {
        private List<Match> matches;

        public List<Match> getMatches() {
            return matches;
        }

        public void setMatches(List<Match> matches) {
            this.matches = matches;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Match {
        private Map<String, String> metadata;

        public Map<String, String> getMetadata() {
            return metadata;
        }
    }
}
