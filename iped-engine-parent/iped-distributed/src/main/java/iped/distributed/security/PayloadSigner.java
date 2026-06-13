package iped.distributed.security;

import iped.distributed.kafka.KafkaItemMessage;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * HMAC-SHA256 signing and verification for {@link KafkaItemMessage} payloads.
 *
 * <p>The signature covers the immutable structural identity fields of a message —
 * the fields that do not change as an item moves through the pipeline:
 * <pre>
 *   "iped-v1" | caseId | itemUuid | pipelineStage | path | lengthBytes
 * </pre>
 * Fields are joined with {@code "|"}.  {@code null} values are represented as the
 * empty string.  The resulting HMAC is stored in {@link KafkaItemMessage#getSignature()}
 * as a lowercase hex string.
 *
 * <p>Usage:
 * <ul>
 *   <li>Before producing a message: call {@link #sign(KafkaItemMessage, String)}.</li>
 *   <li>After consuming a message: call {@link #verify(KafkaItemMessage, String)}.
 *       Returns {@code false} if the signature is absent, tampered, or the wrong secret
 *       is used.</li>
 *   <li>When no secret is configured (empty/blank string): {@link #isEnabled(String)}
 *       returns {@code false}; callers should skip both sign and verify.</li>
 * </ul>
 *
 * <p>A missing signature on a message received when signing is enabled is treated as a
 * verification failure — callers should route such messages to the DLQ.
 */
public final class PayloadSigner {

    private static final String HMAC_ALG = "HmacSHA256";

    private PayloadSigner() {}

    /** True when signing/verification is active (secret is non-blank). */
    public static boolean isEnabled(String secret) {
        return secret != null && !secret.isBlank();
    }

    /**
     * Compute and store the HMAC-SHA256 signature on {@code msg}.
     * After this call {@code msg.getSignature()} carries the hex-encoded HMAC.
     */
    public static void sign(KafkaItemMessage msg, String secret) {
        msg.setSignature(hmac(signingContent(msg), secret));
    }

    /**
     * Verify that {@code msg.getSignature()} matches the expected HMAC for {@code secret}.
     *
     * @return {@code false} if the signature is null/blank, or if HMAC verification fails
     */
    public static boolean verify(KafkaItemMessage msg, String secret) {
        String stored = msg.getSignature();
        if (stored == null || stored.isBlank()) return false;
        String expected = hmac(signingContent(msg), secret);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                stored.getBytes(StandardCharsets.UTF_8));
    }

    // -----------------------------------------------------------------------

    public static String signingContent(KafkaItemMessage msg) {
        return "iped-v1"
                + "|" + nvl(msg.getCaseId())
                + "|" + nvl(msg.getItemUuid())
                + "|" + msg.getPipelineStage()
                + "|" + nvl(msg.getPath())
                + "|" + (msg.getLength() != null ? msg.getLength() : 0);
    }

    static String hmac(String content, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALG));
            byte[] raw = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return toHex(raw);
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    private static String nvl(String s) { return s != null ? s : ""; }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b & 0xFF));
        return sb.toString();
    }
}
