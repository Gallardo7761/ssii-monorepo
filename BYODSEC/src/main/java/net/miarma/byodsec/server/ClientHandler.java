package net.miarma.byodsec.server;

import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import net.miarma.byodsec.common.db.DBConnector;
import net.miarma.byodsec.common.db.dao.SessionDAO;
import net.miarma.byodsec.common.db.dao.UserDAO;
import net.miarma.byodsec.common.db.dto.LoginDTO;
import net.miarma.byodsec.common.db.dto.MessageDTO;
import net.miarma.byodsec.common.db.entities.SessionEntity;
import net.miarma.byodsec.common.db.entities.UserEntity;
import net.miarma.byodsec.common.security.PasswordHasher;
import net.miarma.byodsec.common.socket.SocketRequest;
import net.miarma.byodsec.common.socket.SocketResponse;
import net.miarma.byodsec.common.socket.SocketStatus;
import org.slf4j.Logger;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.UUID;

public class ClientHandler implements Runnable {
    private final SSLSocket socket;
    private final Gson gson;
    private final UserDAO userDAO;
    private final SessionDAO sessionDAO;
    private final DBConnector conn;
    private final Logger logger;

    public ClientHandler(SSLSocket socket, Gson gson, UserDAO userDAO, SessionDAO sessionDAO, DBConnector conn, Logger logger) {
        this.socket = socket;
        this.gson = gson;
        this.userDAO = userDAO;
        this.sessionDAO = sessionDAO;
        this.conn = conn;
        this.logger = logger;
    }

    @Override
    public void run() {
        try (
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        ) {
            logger.info("Nueva conexión desde {}:{}", socket.getInetAddress(), socket.getPort());
            SocketRequest msg = gson.fromJson(input.readLine(), SocketRequest.class);
            SocketResponse res;

            switch (msg.getAction()) {
                case REGISTER -> {
                    LoginDTO dto = gson.fromJson(gson.toJson(msg.getPayload()), LoginDTO.class);
                    logger.info("Procesando REGISTER para {}", dto.getUsername());
                    EntityManager em = conn.getEntityManager();
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
                        conn.close();
                    }
                }

                case LOGIN -> {
                    LoginDTO dto = gson.fromJson(gson.toJson(msg.getPayload()), LoginDTO.class);
                    logger.info("Procesando LOGIN para {}", dto.getUsername());
                    try (EntityManager em = conn.getEntityManager()) {
                        UserEntity user = userDAO.getByUserName(em, dto.getUsername());
                        if (user == null) {
                            res = new SocketResponse(SocketStatus.INVALID_REQUEST, SocketResponse.Message.USER_NOT_FOUND);
                        } else if (PasswordHasher.verify(dto.getPassword(), user.getPassword())) {
                            if(sessionDAO.isLoggedIn(em, user.getUserId())) {
                                res = new SocketResponse(SocketStatus.ALREADY_LOGGED_IN, SocketResponse.Message.SESSION_ACTIVE);
                            } else {
                                res = new SocketResponse(SocketStatus.OK, SocketResponse.Message.LOGGED_IN, user);

                                SessionEntity session = new SessionEntity();
                                session.setSessionId(UUID.randomUUID().toString());
                                session.setUserId(user.getUserId());

                                em.getTransaction().begin();
                                em.persist(session);
                                em.getTransaction().commit();
                            }
                        } else {
                            res = new SocketResponse(SocketStatus.INVALID_REQUEST, SocketResponse.Message.WRONG_PASSWORD);
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                        res = new SocketResponse(SocketStatus.ERROR, SocketResponse.Message.UNKNOWN_ERROR, e.getMessage());
                    }
                }

                case LOGOUT -> {
                    String userId = gson.fromJson(gson.toJson(msg.getPayload()), String.class);
                    logger.info("Procesando LOGOUT para {}", userId);
                    try (EntityManager em = conn.getEntityManager()) {
                        SessionEntity session = sessionDAO.getByUserId(em, userId);
                        if (session != null) {
                            em.getTransaction().begin();
                            em.remove(session);
                            em.getTransaction().commit();
                            res = new SocketResponse(SocketStatus.OK, SocketResponse.Message.LOGGED_OUT);
                        } else {
                            res = new SocketResponse(SocketStatus.SESSION_EXPIRED, SocketResponse.Message.SESSION_EXPIRED);
                        }
                    }
                }

                case SEND_MESSAGE -> {
                    MessageDTO dto = gson.fromJson(gson.toJson(msg.getPayload()), MessageDTO.class);

                    logger.info("Procesando SEND_MESSAGE de {} a Servidor | {}", dto.getFromUserName(), dto.getMessageText());

                    EntityManager em = conn.getEntityManager();
                    try {
                        UserEntity fromUser = userDAO.getByUserName(em, dto.getFromUserName());

                        if (fromUser == null || !sessionDAO.isLoggedIn(em, fromUser.getUserId())) {
                            res = new SocketResponse(SocketStatus.SESSION_EXPIRED, SocketResponse.Message.SESSION_EXPIRED);
                        } else if (dto.getMessageText().isBlank()) {
                            res = new SocketResponse(SocketStatus.INVALID_REQUEST, SocketResponse.Message.INVALID_AMOUNT);
                        } else {
                            res = new SocketResponse(SocketStatus.OK, SocketResponse.Message.MESSAGE_SENT, dto.getMessageText());
                        }

                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                        res = new SocketResponse(SocketStatus.ERROR, SocketResponse.Message.UNKNOWN_ERROR, e.getMessage());
                    } finally {
                        conn.close();
                    }
                }

                default -> {
                    logger.warn("Acción desconocida recibida: {}", msg.getAction());
                    res = new SocketResponse(SocketStatus.INVALID_REQUEST, SocketResponse.Message.UNKNOWN_ERROR);
                }
            }

            output.println(gson.toJson(res));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
