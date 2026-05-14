package iped.engine.hash;

public enum HashAlgorithm {
    MD5("md5"), //$NON-NLS-1$
    SHA1("sha-1"), //$NON-NLS-1$
    SHA256("sha-256"), //$NON-NLS-1$
    SHA512("sha-512"), //$NON-NLS-1$
    EDONKEY("edonkey"); //$NON-NLS-1$

    private final String name;

    HashAlgorithm(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
