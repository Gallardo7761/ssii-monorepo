package net.miarma.byodsec.common.socket;

public class SocketResponse {

    private SocketStatus status;
    private Message message;
    private Object data;

    public SocketResponse(SocketStatus status, Message message) {
        this(status, message, message != null ? message.getMessage() : null);
    }

    public SocketResponse(SocketStatus status, Message message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public SocketStatus getStatus() {
        return status;
    }

    public Message getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        return "SocketResponse{" +
                "status=" + status +
                ", message=" + (message != null ? message.name() : "null") +
                ", data=" + (data != null ? data.toString() : "null") +
                '}';
    }

    public enum Message {
        LOGGED_OUT("Ha cerrado sesión correctamente"),
        LOGGED_IN("Ha iniciado sesión correctamente"),
        MESSAGE_SENT("Su mensaje se ha enviado"),
        USER_NOT_FOUND("El usuario no existe."),
        WRONG_PASSWORD("Contraseña incorrecta."),
        USER_ALREADY_EXISTS("El usuario ya está registrado."),
        CONNECTION_ERROR("Error de conexión con el servidor."),
        UNKNOWN_ERROR("Ha ocurrido un error inesperado."),
        SESSION_EXPIRED("Tu sesión ha expirado."),
        INTEGRITY_FAIL("Fallo de integridad en el mensaje."),
        INVALID_AMOUNT("Cantidad no válida."),
        REPLAY_ATTACK("Intento de repetición detectado."),
        RECIPIENT_NOT_FOUND("El destinatario no existe"),
        REGISTERED("Se ha registrado correctamente"),
        SESSION_ACTIVE("Ya hay una sesión activa para este usuario.");

        private final String message;

        Message(String userMessage) {
            this.message = userMessage;
        }

        public String getMessage() {
            return message;
        }
    }
}
