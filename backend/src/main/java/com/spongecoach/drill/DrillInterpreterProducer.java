package com.spongecoach.drill;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

/**
 * Makes the Claude-backed interpreter the application's {@link DrillInterpreter}. Without an API
 * key every job fails with a readable error, and the rest of the app works as before (ADR-0019).
 */
@ApplicationScoped
public class DrillInterpreterProducer {

    @ConfigProperty(name = "spongecoach.anthropic.api-key")
    Optional<String> apiKey;

    @ConfigProperty(name = "spongecoach.anthropic.model")
    String model;

    @Produces
    @ApplicationScoped
    DrillInterpreter interpreter() {
        if (apiKey.isEmpty() || apiKey.get().isBlank()) {
            return input -> {
                throw new DrillUnavailableException("Kein Anthropic-API-Key konfiguriert (ANTHROPIC_API_KEY).");
            };
        }
        return new AnthropicDrillInterpreter(apiKey.get(), model);
    }
}
