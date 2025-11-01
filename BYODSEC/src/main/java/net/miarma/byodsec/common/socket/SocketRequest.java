package net.miarma.byodsec.common.socket;

public class SocketRequest {
    private SocketRequest.Action action;
    private Object payload;

    public SocketRequest() {}

    public SocketRequest(SocketRequest.Action action, Object payload) {
        this.action = action;
        this.payload = payload;
    }

    public SocketRequest.Action getAction() {
        return action;
    }

    public void setAction(SocketRequest.Action action) {
        this.action = action;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "SocketRequest{" +
                "action='" + action.name() + '\'' +
                ", payload=" + (payload != null ? payload.toString() : "null") +
                '}';
    }

    public enum Action {
        LOGIN, REGISTER, SEND_MESSAGE, LOGOUT
    }
}
