package com.example.application;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.util.Set;

/**
 * The entry point of the Spring Boot application.
 * <p>
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 */
@SpringBootApplication
@Theme(value = "vaadin-docs-assistant")
@ImportRuntimeHints(Application.Hints.class)
public class Application implements AppShellConfigurator {

    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            for (var t : Set.of(com.google.protobuf.Value.class,
                    com.google.protobuf.Value.Builder.class,
                    com.google.protobuf.Struct.class)) {
                hints.reflection().registerType(t, MemberCategory.values());
            }
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}