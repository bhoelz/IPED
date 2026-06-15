package iped.runner.distributed;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * HMAC-SHA256 payload signing for Kafka item messages.
 *
 * <p>Signs the immutable structural identity of a message as:
 * <pre>
 *   iped-v1 | caseId | itemUuid | pipelineStage | itemPath | lengthBytes
 * </pre>
 * Fields joined with {@code |}, null fields rendered as empty string.
 * Result is lowercase hex stored in {@link StatusEvent} (or a dedicated signature field).
 *
 * <p>When {@code payloadSigningSecret} is blank both {@link #sign} and {@link #verify}
 * are no-ops, enabling a rolling upgrade path: disable verification until all agents
 * are updated, then re-enable.
 */
public final class PayloadSigner {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String IPED_V1   = "iped-v1";

    private PayloadSigner() {}

    /** Returns true when payload signing is active (secret is non-blank). */
    public static boolean isEnabled(String secret) {
        return secret != null && !secret.isBlank();
    }

    /**
     * Computes the HMAC-SHA256 signature for the given fields.
     *
     * @param secret shared secret; must not be blank when calling this method
     * @return lowercase hex signature string
     */
    public static String sign(String secret,
                              String caseId,
                              String itemUuid,
                              int    pipelineStage,
                              String itemPath,
                              long   lengthBytes) {
        String payload = buildPayload(caseId, itemUuid, pipelineStage, itemPath, lengthBytes);
        return hmacHex(secret, payload);
    }

    /**
     * Verifies that {@code signature} matches the HMAC of the given fields.
     *
     * @return true when the signature is valid; false on mismatch or null input
     */
    public static boolean verify(String secret,
                                 String signature,
                                 String caseId,
                                 String itemUuid,
                                 int    pipelineStage,
                                 String itemPath,
                                 long   lengthBytes) {
        if (signature == null || signature.isBlank()) return false;
        String expected = sign(secret, caseId, itemUuid, pipelineStage, itemPath, lengthBytes);
        return constantTimeEquals(expected, signature);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    static String buildPayload(String caseId, String itemUuid,
                               int pipelineStage, String itemPath, long lengthBytes) {
        return IPED_V1 + "|"
                + nullSafe(caseId)    + "|"
                + nullSafe(itemUuid)  + "|"
                + pipelineStage       + "|"
                + nullSafe(itemPath)  + "|"
                + lengthBytes;
    }

    private static String nullSafe(String s) { return s != null ? s : ""; }

    private static String hmacHex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC-SHA256 unavailable", e);
        }
    }

    /** Constant-time string comparison to prevent timing attacks on hex signatures. */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
