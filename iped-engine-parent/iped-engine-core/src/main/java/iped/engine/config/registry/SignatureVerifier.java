package iped.engine.config.registry;

import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

final class SignatureVerifier {

    private SignatureVerifier() {
    }

    static boolean verifyDetached(String type, PublicKey key, byte[] data, String signature) {
        if ("none".equalsIgnoreCase(type)) {
            return true;
        }
        if (!"SHA256withRSA".equalsIgnoreCase(type)) {
            throw new IllegalArgumentException("Unsupported signature type: " + type);
        }

        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update(data);
            byte[] sigBytes = Base64.getDecoder().decode(signature.trim());
            return verifier.verify(sigBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Signature verification failed", e);
        }
    }
}
