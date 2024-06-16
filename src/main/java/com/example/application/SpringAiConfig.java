package com.example.application;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.PineconeVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiConfig {

    @Value("${pinecone.api.key}")
    private String apiKey;

    @Value("${pinecone.environment}")
    String environment;

    @Value("${pinecone.project}")
    String projectId;

    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    @Bean
    @Qualifier("hilla")
    public VectorStore hilla(EmbeddingModel embeddingModel) {
        return new PineconeVectorStore(pineconeVectorStoreConfig("react"), embeddingModel);
    }

    @Bean
    @Qualifier("flow")
    public VectorStore flow(EmbeddingModel embeddingModel) {
        return new PineconeVectorStore(pineconeVectorStoreConfig("flow"), embeddingModel);
    }

    private PineconeVectorStore.PineconeVectorStoreConfig pineconeVectorStoreConfig(String framework) {
        return PineconeVectorStore.PineconeVectorStoreConfig.builder()
                .withApiKey(apiKey)
                .withEnvironment(environment)
                .withProjectId(projectId)
                .withIndexName("docs")
                .withNamespace(framework)
                .build();
    }
}
