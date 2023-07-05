package com.example.application.service.openai.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionMessage {

    public enum Role {
        SYSTEM("system"),
        USER("user"),
        ASSISTANT("assistant");

        private final String role;

        Role(String role) {
            this.role = role;
        }

        @Override
        @JsonValue
        public String toString() {
            return role;
        }

        @JsonCreator
        public static Role fromString(String value) {
            return Role.valueOf(value.toUpperCase());
        }
    }

    private Role role;
    private String content;

    public ChatCompletionMessage() {
    }

    public ChatCompletionMessage(Role role, String content) {
        this.role = role;
        this.content = content;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "ChatCompletionMessage{" +
                "role=" + role +
                ", content='" + content + '\'' +
                '}';
    }
}
