package com.example.application.endpoints;

import com.example.application.service.DocsAssistantService;
import com.example.application.service.Framework;
import com.example.application.service.openai.model.ChatCompletionMessage;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import dev.hilla.Endpoint;
import reactor.core.publisher.Flux;

import java.util.List;

@Endpoint
@AnonymousAllowed
public class DocsAssistantEndpoint {

    private final DocsAssistantService docsAssistantService;

    public DocsAssistantEndpoint(DocsAssistantService docsAssistantService) {
        this.docsAssistantService = docsAssistantService;
    }

    public Flux<String> getCompletionStream(List<ChatCompletionMessage> history, String framework) {
        return docsAssistantService.getCompletionStream(history, framework);
    }

    public List<Framework> getSupportedFrameworks() {
        return docsAssistantService.getSupportedFrameworks();
    }
}
