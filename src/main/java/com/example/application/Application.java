package com.example.application;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * The entry point of the Spring Boot application.
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 */
@SpringBootApplication
@ImportRuntimeHints(Application.RuntimeHints.class)
//@PWA(name = "Vaadin Docs Assistant", shortName = "Vaadin Assistant")
public class Application implements AppShellConfigurator {

    // Register runtime hints for the token library
    public static class RuntimeHints implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(org.springframework.aot.hint.RuntimeHints hints, ClassLoader classLoader) {
            hints.resources().registerPattern("com/knuddels/jtokkit/*.tiktoken");
        }
    }


    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
