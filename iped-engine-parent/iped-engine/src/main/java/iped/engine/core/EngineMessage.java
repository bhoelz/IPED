package iped.engine.core;

/**
 * Carries a titled message from the engine to the UI layer via the
 * {@link iped.engine.util.UIPropertyListenerProvider} event bus
 * (property name {@code "uiWarning"}).
 */
public record EngineMessage(String title, String body) {}
