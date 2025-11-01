package net.miarma.byodsec.common.socket;

public enum SocketStatus {
    OK,
    ERROR,
    SESSION_EXPIRED,
    ALREADY_LOGGED_IN,
    INTEGRITY_FAIL,
    UNAUTHORIZED,
    INVALID_REQUEST;

    public static boolean isOk(String status) {
        return OK.name().equalsIgnoreCase(status);
    }
}
