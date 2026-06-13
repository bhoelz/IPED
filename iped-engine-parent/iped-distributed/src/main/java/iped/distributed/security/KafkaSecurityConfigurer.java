package iped.distributed.security;

import iped.distributed.config.DistributedConfig;

import java.util.Properties;

/**
 * Applies Kafka security configuration (TLS / SASL) to a {@link Properties} map that
 * will be passed to a Kafka producer, consumer, or AdminClient.
 *
 * <p>Supported configurations (controlled by {@link DistributedConfig}):
 * <ul>
 *   <li><b>Plain (no auth)</b> — {@code kafkaTlsEnabled=false}, blank SASL mechanism:
 *       no changes to the properties.</li>
 *   <li><b>TLS only</b> — {@code kafkaTlsEnabled=true}, blank SASL mechanism:
 *       sets {@code security.protocol=SSL}; optional truststore and keystore paths.</li>
 *   <li><b>SASL over plaintext</b> — {@code kafkaTlsEnabled=false}, SASL mechanism set:
 *       sets {@code security.protocol=SASL_PLAINTEXT}.</li>
 *   <li><b>SASL over TLS</b> — {@code kafkaTlsEnabled=true}, SASL mechanism set:
 *       sets {@code security.protocol=SASL_SSL} plus all TLS and SASL properties.</li>
 * </ul>
 *
 * <p>Supported SASL mechanisms: {@code PLAIN}, {@code SCRAM-SHA-256}, {@code SCRAM-SHA-512}.
 */
public final class KafkaSecurityConfigurer {

    private KafkaSecurityConfigurer() {}

    /**
     * Apply security properties derived from {@code cfg} to {@code props}.
     * No-op when neither TLS nor SASL is configured.
     */
    public static void apply(Properties props, DistributedConfig cfg) {
        boolean tls  = cfg.isKafkaTlsEnabled();
        boolean sasl = cfg.getKafkaSaslMechanism() != null && !cfg.getKafkaSaslMechanism().isBlank();

        if (!tls && !sasl) return;

        // Security protocol
        String protocol;
        if (tls && sasl)       protocol = "SASL_SSL";
        else if (tls)          protocol = "SSL";
        else                   protocol = "SASL_PLAINTEXT";
        props.put("security.protocol", protocol);

        // TLS: truststore (server cert verification) + optional keystore (mTLS)
        if (tls) {
            if (!cfg.getKafkaTruststorePath().isBlank()) {
                props.put("ssl.truststore.location", cfg.getKafkaTruststorePath());
                props.put("ssl.truststore.password", cfg.getKafkaTruststorePassword());
            }
            if (!cfg.getKafkaKeystorePath().isBlank()) {
                props.put("ssl.keystore.location",  cfg.getKafkaKeystorePath());
                props.put("ssl.keystore.password",  cfg.getKafkaKeystorePassword());
                props.put("ssl.key.password",       cfg.getKafkaKeyPassword());
            }
        }

        // SASL
        if (sasl) {
            String mechanism = cfg.getKafkaSaslMechanism().toUpperCase();
            props.put("sasl.mechanism", mechanism);
            props.put("sasl.jaas.config", buildJaas(mechanism,
                    cfg.getKafkaSaslUsername(), cfg.getKafkaSaslPassword()));
        }
    }

    /**
     * Build a JAAS login module config string for the given mechanism and credentials.
     *
     * @throws IllegalArgumentException for unrecognised mechanisms
     */
    public static String buildJaas(String mechanism, String username, String password) {
        String safeUser = escape(username);
        String safePass = escape(password);
        return switch (mechanism.toUpperCase()) {
            case "PLAIN" ->
                    "org.apache.kafka.common.security.plain.PlainLoginModule required"
                    + " username=\"" + safeUser + "\""
                    + " password=\"" + safePass + "\";";
            case "SCRAM-SHA-256", "SCRAM-SHA-512" ->
                    "org.apache.kafka.common.security.scram.ScramLoginModule required"
                    + " username=\"" + safeUser + "\""
                    + " password=\"" + safePass + "\";";
            default -> throw new IllegalArgumentException(
                    "Unsupported SASL mechanism: " + mechanism
                    + " (supported: PLAIN, SCRAM-SHA-256, SCRAM-SHA-512)");
        };
    }

    /** Escape double-quotes in JAAS config values (simple pass-through — production values should not contain them). */
    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
