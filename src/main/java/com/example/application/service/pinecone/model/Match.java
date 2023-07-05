package com.example.application.service.pinecone.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Match {
    private Map<String, String> metadata;

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
