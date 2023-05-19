package com.example.application.service.openai.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Choice {
    private Message message;
    @JsonProperty("finish_reason")
    private String finishReason;
    private int index;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
