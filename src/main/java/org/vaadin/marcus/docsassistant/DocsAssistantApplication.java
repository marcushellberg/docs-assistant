package org.vaadin.marcus.docsassistant;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.util.Set;

@SpringBootApplication
@ImportRuntimeHints(DocsAssistantApplication.Hints.class)
public class DocsAssistantApplication {

    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            for (var type : Set.of(
                org.springframework.ai.openai.OpenAiChatOptions.class,

                // From the Pinecone depdendency. Registering the hints required adding gson to pom.xml to work
                org.openapitools.db_control.client.model.IndexModel.class,
                org.openapitools.db_control.client.model.IndexModel.MetricEnum.class,
                org.openapitools.db_control.client.model.IndexModel.MetricEnum.Adapter.class,
                org.openapitools.db_control.client.model.DeletionProtection.class,
                org.openapitools.db_control.client.model.DeletionProtection.Adapter.class,
                org.openapitools.db_control.client.model.IndexModelStatus.class,
                org.openapitools.db_control.client.model.IndexModelStatus.StateEnum.class,
                org.openapitools.db_control.client.model.IndexModelStatus.StateEnum.Adapter.class,
                org.openapitools.db_control.client.model.IndexModelSpec.class,
                org.openapitools.db_control.client.model.ServerlessSpec.class,
                org.openapitools.db_control.client.model.ServerlessSpec.CloudEnum.class,
                org.openapitools.db_control.client.model.ServerlessSpec.CloudEnum.Adapter.class
            )) {
                hints.reflection().registerType(type, MemberCategory.values());
            }
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(DocsAssistantApplication.class, args);
    }

}
