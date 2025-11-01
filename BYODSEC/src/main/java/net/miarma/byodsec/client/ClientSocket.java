package net.miarma.byodsec.client;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;

import javax.net.ssl.*;

import com.google.gson.Gson;

import net.miarma.byodsec.common.ConfigManager;
import net.miarma.byodsec.common.db.dto.LoginDTO;
import net.miarma.byodsec.common.db.dto.MessageDTO;
import net.miarma.byodsec.common.socket.SocketRequest;
import net.miarma.byodsec.common.socket.SocketResponse;

public class ClientSocket implements AutoCloseable {
    private SSLSocket socket;
    private PrintWriter output;
    private BufferedReader input;
    private final Gson gson = new Gson();
    private final ConfigManager configManager = ConfigManager.getInstance();

    public ClientSocket() throws IOException {
        configManager.loadConfig();

        // inicializamos el socket SSL, la versión de TLS y los Cipher Suites a utilizar
        SSLSocketFactory factory;
        try {
            factory = initSSL();
        } catch (Exception e) {
            throw new IOException("Error initializing SSL context.", e);
        }

        this.socket = (SSLSocket) factory.createSocket("localhost", 6969);
        this.socket.setEnabledProtocols(new String[]{ "TLSv1.3" });
        this.socket.setEnabledCipherSuites(new String[]{
                "TLS_AES_256_GCM_SHA384",
                "TLS_AES_128_GCM_SHA256",
                "TLS_CHACHA20_POLY1305_SHA256"
        });

        // buffers
        this.output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private SSLSocketFactory initSSL() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(configManager.getJksFile().getAbsolutePath()),
                configManager.getStringProperty("jksPassword").toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, configManager.getStringProperty("jksPassword").toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslContext.getSocketFactory();
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

    public SocketResponse sendMessage(MessageDTO message) throws Exception {
        SocketRequest msg = new SocketRequest(SocketRequest.Action.SEND_MESSAGE, message);
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
