package iped.runner.distributed;

import java.util.Properties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;

/**
 * Applies TLS and SASL authentication properties to any Kafka client {@link Properties} map.
 *
 * <p>Supported security.protocol modes:
 *
 * <table>
 *   <tr><th>TLS enabled</th><th>SASL mechanism</th><th>protocol</th></tr>
 *   <tr><td>false</td><td>(blank)</td><td>PLAINTEXT (default)</td></tr>
 *   <tr><td>true</td><td>(blank)</td><td>SSL</td></tr>
 *   <tr><td>false</td><td>PLAIN / SCRAM-*</td><td>SASL_PLAINTEXT</td></tr>
 *   <tr><td>true</td><td>PLAIN / SCRAM-*</td><td>SASL_SSL</td></tr>
 * </table>
 *
 * <p>Call {@link #apply(Properties, KafkaSecurityConfig)} before constructing any {@code
 * KafkaProducer}, {@code KafkaConsumer}, or {@code AdminClient}.
 */
public final class KafkaSecurityConfigurer {

  private KafkaSecurityConfigurer() {}

  /**
   * Applies security properties derived from {@code cfg} to {@code props}. No-op when TLS is
   * disabled and no SASL mechanism is set.
   *
   * @throws IllegalArgumentException when an unsupported SASL mechanism is supplied.
   */
  public static void apply(Properties props, KafkaSecurityConfig cfg) {
    boolean hasSasl = cfg.saslMechanism() != null && !cfg.saslMechanism().isBlank();

    // ── security.protocol ───────────────────────────────────────────────
    String protocol;
    if (cfg.tlsEnabled() && hasSasl) protocol = "SASL_SSL";
    else if (cfg.tlsEnabled()) protocol = "SSL";
    else if (hasSasl) protocol = "SASL_PLAINTEXT";
    else return; // PLAINTEXT — nothing to configure

    props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, protocol);

    // ── TLS ─────────────────────────────────────────────────────────────
    if (cfg.tlsEnabled()) {
      setIfPresent(props, SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, cfg.truststorePath());
      setIfPresent(props, SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, cfg.truststorePassword());
      setIfPresent(props, SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, cfg.keystorePath());
      setIfPresent(props, SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, cfg.keystorePassword());
      setIfPresent(props, SslConfigs.SSL_KEY_PASSWORD_CONFIG, cfg.keyPassword());
    }

    // ── SASL ─────────────────────────────────────────────────────────────
    if (hasSasl) {
      String mechanism = cfg.saslMechanism().toUpperCase();
      String jaasConfig = buildJaasConfig(mechanism, cfg.saslUsername(), cfg.saslPassword());
      props.put(SaslConfigs.SASL_MECHANISM, mechanism);
      props.put(SaslConfigs.SASL_JAAS_CONFIG, jaasConfig);
    }
  }

  private static String buildJaasConfig(String mechanism, String username, String password) {
    String u = username != null ? username : "";
    String p = password != null ? password : "";
    return switch (mechanism) {
      case "PLAIN" ->
          "org.apache.kafka.common.security.plain.PlainLoginModule required"
              + " username=\""
              + u
              + "\" password=\""
              + p
              + "\";";
      case "SCRAM-SHA-256" ->
          "org.apache.kafka.common.security.scram.ScramLoginModule required"
              + " username=\""
              + u
              + "\" password=\""
              + p
              + "\";";
      case "SCRAM-SHA-512" ->
          "org.apache.kafka.common.security.scram.ScramLoginModule required"
              + " username=\""
              + u
              + "\" password=\""
              + p
              + "\";";
      default ->
          throw new IllegalArgumentException(
              "Unsupported SASL mechanism: "
                  + mechanism
                  + ". Supported: PLAIN, SCRAM-SHA-256, SCRAM-SHA-512");
    };
  }

  private static void setIfPresent(Properties props, String key, String value) {
    if (value != null && !value.isBlank()) {
      props.put(key, value);
    }
  }

  /**
   * Kafka security configuration value object. Immutable; construct with the builder or record
   * syntax.
   */
  public record KafkaSecurityConfig(
      boolean tlsEnabled,
      String truststorePath,
      String truststorePassword,
      String keystorePath,
      String keystorePassword,
      String keyPassword,
      String saslMechanism,
      String saslUsername,
      String saslPassword) {
    /** All-blank / disabled config — PLAINTEXT mode. */
    public static KafkaSecurityConfig disabled() {
      return new KafkaSecurityConfig(false, "", "", "", "", "", "", "", "");
    }
  }
}
