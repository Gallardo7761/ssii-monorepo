package net.miarma.integridos.client;

import com.google.gson.Gson;
import net.miarma.integridos.common.*;
import net.miarma.integridos.common.security.IntegrityProvider;
import net.miarma.integridos.common.socket.SocketRequest;
import net.miarma.integridos.common.socket.SocketResponse;
import net.miarma.integridos.common.db.dto.LoginDTO;
import net.miarma.integridos.common.db.dto.TransactionDTO;

import java.io.*;
import java.net.Socket;

public class ClientSocket implements AutoCloseable {
    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;
    private final Gson gson = new Gson();

    public ClientSocket() throws IOException {
        this.socket = new Socket("localhost", 6969);
        this.output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public SocketResponse login(String username, String password) throws IOException {
        SocketRequest msg = new SocketRequest(SocketRequest.Action.LOGIN, new LoginDTO(username, password));
        output.println(gson.toJson(msg));
        return gson.fromJson(input.readLine(), SocketResponse.class);
    }

    public SocketResponse register(String username, String password) throws IOException {
        SocketRequest msg = new SocketRequest(SocketRequest.Action.REGISTER, new LoginDTO(username, password));
        output.println(gson.toJson(msg));
        return gson.fromJson(input.readLine(), SocketResponse.class);
    }

    public SocketResponse logout(String userId) throws IOException {
        SocketRequest msg = new SocketRequest(SocketRequest.Action.LOGOUT, userId);
        output.println(gson.toJson(msg));
        return gson.fromJson(input.readLine(), SocketResponse.class);
    }

    public SocketResponse sendTransaction(TransactionDTO transaction) throws Exception {
        transaction.setHmac(null);
        SocketRequest msg = new SocketRequest(SocketRequest.Action.SEND_TRANSACTION, transaction);
        String jsonMsg = gson.toJson(msg);
        String hmac = IntegrityProvider.generateHMAC(ConfigManager.getInstance().getStringProperty("secret"), jsonMsg);
        transaction.setHmac(hmac);
        msg.setPayload(transaction);
        output.println(gson.toJson(msg));
        return gson.fromJson(input.readLine(), SocketResponse.class);
    }

    @Override
    public void close() {
        try {
            input.close();
            output.close();
            socket.close();
        } catch (Exception ignored) {}
    }
}
