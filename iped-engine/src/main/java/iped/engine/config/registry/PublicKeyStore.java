package iped.engine.config.registry;

import java.security.PublicKey;

public interface PublicKeyStore {
    PublicKey find(String keyId);
}
