package net.miarma.integridos.server;

import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import net.miarma.integridos.common.*;
import net.miarma.integridos.common.db.entities.TransactionEntity;
import net.miarma.integridos.common.security.IntegrityProvider;
import net.miarma.integridos.common.security.PasswordHasher;
import net.miarma.integridos.common.socket.SocketRequest;
import net.miarma.integridos.common.socket.SocketResponse;
import net.miarma.integridos.common.socket.SocketStatus;
import net.miarma.integridos.common.db.DBConnector;
import net.miarma.integridos.common.db.dao.SessionDAO;
import net.miarma.integridos.common.db.dao.TransactionDAO;
import net.miarma.integridos.common.db.dao.UserDAO;
import net.miarma.integridos.common.db.dto.LoginDTO;
import net.miarma.integridos.common.db.dto.TransactionDTO;
import net.miarma.integridos.common.db.entities.SessionEntity;
import net.miarma.integridos.common.db.entities.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

public class RemoteSocket {
    static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(6969);
        DBConnector conn = DBConnector.getInstance();
        UserDAO userDAO = new UserDAO();
        SessionDAO sessionDAO = new SessionDAO();
        TransactionDAO transactionDAO = new TransactionDAO();
        Gson gson = new Gson();
        final Logger logger = LoggerFactory.getLogger(RemoteSocket.class);
        ConfigManager.getInstance().loadConfig();

        while (true) {
            Socket socket = serverSocket.accept();
            logger.info("Nueva conexión desde {}:{}", socket.getInetAddress(), socket.getPort());
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            try {
                SocketRequest msg = gson.fromJson(input.readLine(), SocketRequest.class);
                SocketResponse res;

                switch (msg.getAction()) {
                    case REGISTER -> {
                        LoginDTO dto = gson.fromJson(gson.toJson(msg.getPayload()), LoginDTO.class);
                        logger.info("Procesando REGISTER para {}", dto.getUsername());
                        EntityManager em = conn.createEntityManager();
                        try {
                            if (userDAO.getByUserName(em, dto.getUsername()) != null) {
                                res = new SocketResponse(SocketStatus.INVALID_REQUEST, SocketResponse.Message.USER_ALREADY_EXISTS);
                            } else {
                                em.getTransaction().begin();
                                UserEntity u = new UserEntity(UUID.randomUUID().toString(),
                                        dto.getUsername(), PasswordHasher.hash(dto.getPassword()));
                                em.persist(u);
                                em.getTransaction().commit();
                                res = new SocketResponse(SocketStatus.OK, SocketResponse.Message.REGISTERED, u);
                            }
                        } catch (Exception e) {
                            if (em.getTransaction().isActive()) em.getTransaction().rollback();
                            logger.error(e.getMessage(), e);
                            res = new SocketResponse(SocketStatus.ERROR, SocketResponse.Message.UNKNOWN_ERROR, e.getMessage());
                        } finally {
                            em.close();
                        }
                    }

                    case LOGIN -> {
                        LoginDTO dto = gson.fromJson(gson.toJson(msg.getPayload()), LoginDTO.class);
                        logger.info("Procesando LOGIN para {}", dto.getUsername());
                        EntityManager em = conn.createEntityManager();
                        try {
                            UserEntity user = userDAO.getByUserName(em, dto.getUsername());
                            if (user == null) {
                                res = new SocketResponse(SocketStatus.INVALID_REQUEST, SocketResponse.Message.USER_NOT_FOUND);
                            } else if (PasswordHasher.verify(dto.getPassword(), user.getPassword())) {
                                res = new SocketResponse(SocketStatus.OK, SocketResponse.Message.LOGGED_IN, user);

                                SessionEntity session = new SessionEntity();
                                session.setSessionId(UUID.randomUUID().toString());
                                session.setUserId(user.getUserId());

                                em.getTransaction().begin();
                                em.persist(session);
                                em.getTransaction().commit();
                            } else {
                                res = new SocketResponse(SocketStatus.INVALID_REQUEST, SocketResponse.Message.WRONG_PASSWORD);
                            }
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                            res = new SocketResponse(SocketStatus.ERROR, SocketResponse.Message.UNKNOWN_ERROR, e.getMessage());
                        } finally {
                            em.close();
                        }
                    }

                    case LOGOUT -> {
                        String userId = gson.fromJson(gson.toJson(msg.getPayload()), String.class);
                        logger.info("Procesando LOGOUT para {}", userId);
                        EntityManager em = conn.createEntityManager();
                        try {
                            SessionEntity session = sessionDAO.getByUserId(em, userId);
                            if (session != null) {
                                em.getTransaction().begin();
                                em.remove(session);
                                em.getTransaction().commit();
                                res = new SocketResponse(SocketStatus.OK, SocketResponse.Message.LOGGED_OUT);
                            } else {
                                res = new SocketResponse(SocketStatus.SESSION_EXPIRED, SocketResponse.Message.SESSION_EXPIRED);
                            }
                        } finally {
                            em.close();
                        }
                    }

                    case SEND_TRANSACTION -> {
                        TransactionDTO dto = gson.fromJson(gson.toJson(msg.getPayload()), TransactionDTO.class);
                        
                        logger.info("Procesando SEND_TRANSACTION de {} a {} | {}", dto.getFromUserName(), dto.getToUserName(), dto.getAmount());
                        
                        String receivedHmac = dto.getHmac();
                        
                        logger.debug("HMAC recibida: {}", receivedHmac);
                        
                        dto.setHmac(null);
                        msg.setPayload(dto);
                        String jsonMsg = gson.toJson(msg);
                        
                        logger.debug("Mensaje JSON para calcular HMAC: {}", jsonMsg);

                        String generatedHmac = IntegrityProvider.generateHMAC(
                            ConfigManager.getInstance().getStringProperty("secret"), jsonMsg
                        );
                        
                        logger.debug("HMAC generada: {}", generatedHmac);

                        if (!generatedHmac.equals(receivedHmac)) {
                            res = new SocketResponse(SocketStatus.INTEGRITY_FAIL, SocketResponse.Message.INTEGRITY_FAIL);
                            break;
                        }
                        
                        logger.debug("Las HMAC coinciden");

                        EntityManager em = conn.createEntityManager();
                        try {
                            if (transactionDAO.isNonceUsed(em, dto.getNonce())) {
                                res = new SocketResponse(SocketStatus.INTEGRITY_FAIL, SocketResponse.Message.REPLAY_ATTACK);
                                break;
                            }

                            em.getTransaction().begin();
                            UserEntity fromUser = userDAO.getByUserName(em, dto.getFromUserName());
                            UserEntity toUser = userDAO.getByUserName(em, dto.getToUserName());

                            if (fromUser == null || !sessionDAO.isLoggedIn(em, fromUser.getUserId())) {
                                res = new SocketResponse(SocketStatus.SESSION_EXPIRED, SocketResponse.Message.SESSION_EXPIRED);
                            } else if (toUser == null) {
                                res = new SocketResponse(SocketStatus.INVALID_REQUEST, SocketResponse.Message.RECIPIENT_NOT_FOUND);
                            } else if (dto.getAmount() <= 0 || dto.getAmount() > fromUser.getBalance()) {
                                res = new SocketResponse(SocketStatus.INVALID_REQUEST, SocketResponse.Message.INVALID_AMOUNT);
                            } else {
                                TransactionEntity t = new TransactionEntity(UUID.randomUUID().toString(), fromUser, toUser, dto.getAmount());
                                t.setNonce(dto.getNonce());

                                fromUser.setBalance(fromUser.getBalance() - dto.getAmount());
                                toUser.setBalance(toUser.getBalance() + dto.getAmount());

                                em.persist(t);
                                em.merge(fromUser);
                                em.merge(toUser);

                                res = new SocketResponse(SocketStatus.OK, SocketResponse.Message.TRANSACTION_SENT, t);
                            }

                            em.getTransaction().commit();
                        } catch (Exception e) {
                            if (em.getTransaction().isActive()) em.getTransaction().rollback();
                            logger.error(e.getMessage(), e);
                            res = new SocketResponse(SocketStatus.ERROR, SocketResponse.Message.UNKNOWN_ERROR, e.getMessage());
                        } finally {
                            em.close();
                        }
                    }

                    default -> {
                        logger.warn("Acción desconocida recibida: {}", msg.getAction());
                        res = new SocketResponse(SocketStatus.INVALID_REQUEST, SocketResponse.Message.UNKNOWN_ERROR);
                    }
                }

                output.println(gson.toJson(res));
            } catch (Exception e) {
                logger.error("Fallo procesando solicitud", e);
            } finally {
                input.close();
                output.close();
                socket.close();
            }
        }
    }
}
