package iped.configuration;

/**
 * Implemented by configurables and tasks whose execution can be switched on or
 * off via configuration.
 */
public interface EnabledInterface {

    /**
     * @return {@code true} if this component is enabled and should be executed
     */
    boolean isEnabled();

    /**
     * Enables or disables this component.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     */
    void setEnabled(boolean enabled);

}
