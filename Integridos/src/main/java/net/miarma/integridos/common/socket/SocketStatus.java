package net.miarma.integridos.common.socket;

public enum SocketStatus {
    OK,
    ERROR,
    SESSION_EXPIRED,
    INTEGRITY_FAIL,
    UNAUTHORIZED,
    INVALID_REQUEST;

    public static boolean isOk(String status) {
        return OK.name().equalsIgnoreCase(status);
    }
}
