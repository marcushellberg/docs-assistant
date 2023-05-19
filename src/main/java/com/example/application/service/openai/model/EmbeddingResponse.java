package com.example.application.service.openai.model;

import java.util.List;

public class EmbeddingResponse {
    private List<EmbeddingData> data;

    public List<Double> getEmbedding() {
        return data.get(0).getEmbedding();
    }

    public List<EmbeddingData> getData() {
        return data;
    }

    public void setData(List<EmbeddingData> data) {
        this.data = data;
    }
}