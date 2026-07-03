package iped.osint.spi;

public record OsintPolicyDecision(boolean allowed, String reason) {

    public static OsintPolicyDecision allow() {
        return new OsintPolicyDecision(true, "allowed");
    }

    public static OsintPolicyDecision deny(String reason) {
        return new OsintPolicyDecision(false, reason);
    }
}
