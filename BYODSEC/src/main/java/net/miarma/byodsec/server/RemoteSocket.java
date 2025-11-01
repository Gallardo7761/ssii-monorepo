package net.miarma.byodsec.server;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import net.miarma.byodsec.common.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import net.miarma.byodsec.common.db.DBConnector;
import net.miarma.byodsec.common.db.dao.SessionDAO;
import net.miarma.byodsec.common.db.dao.UserDAO;

public class RemoteSocket {
    static void main(String[] args) throws IOException {
        ConfigManager configManager = ConfigManager.getInstance();
        configManager.loadConfig();
        DBConnector conn = DBConnector.getInstance();
        UserDAO userDAO = new UserDAO();
        SessionDAO sessionDAO = new SessionDAO();
        Gson gson = new Gson();
        final Logger logger = LoggerFactory.getLogger(RemoteSocket.class);

        // especificamos mediante propiedades el KS y la contraseña
        System.setProperty("javax.net.ssl.keyStore", configManager.getJksFile().getAbsolutePath());
        System.setProperty("javax.net.ssl.keyStorePassword", configManager.getStringProperty("jksPassword"));
        System.setProperty("javax.net.ssl.trustStore", configManager.getJksFile().getAbsolutePath());
        System.setProperty("javax.net.ssl.trustStorePassword", configManager.getStringProperty("jksPassword"));

        // inicializamos el socket SSL, la versión de TLS y los Cipher Suites a utilizar
        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
    	SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(6969);
        serverSocket.setEnabledProtocols(new String[]{ "TLSv1.3" });
        serverSocket.setEnabledCipherSuites(new String[]{
            "TLS_AES_256_GCM_SHA384",
            "TLS_AES_128_GCM_SHA256",
            "TLS_CHACHA20_POLY1305_SHA256"
        });
        serverSocket.setNeedClientAuth(true);

        // main loop
        Executor executor = Executors.newFixedThreadPool(300);
        while (true) {
            SSLSocket socket = (SSLSocket) serverSocket.accept();
            executor.execute(new ClientHandler(socket, gson, userDAO, sessionDAO, conn, logger));
        }
    }
}
