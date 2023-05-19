package com.example.application.service.openai.model;

public class ModerationRequest {
    private String input;

    public ModerationRequest(String input) {
        this.input = input;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }
}