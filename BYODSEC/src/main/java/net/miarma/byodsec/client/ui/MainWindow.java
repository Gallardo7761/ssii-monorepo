/*
 * Created by JFormDesigner on Wed Oct 01 16:21:10 CEST 2025
 */

package net.miarma.byodsec.client.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.*;

import com.google.gson.Gson;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import net.miarma.byodsec.common.Constants;

import net.miarma.byodsec.common.security.IntegrityProvider;
import net.miarma.byodsec.common.socket.SocketResponse;
import net.miarma.byodsec.common.socket.SocketStatus;
import net.miarma.byodsec.common.db.dto.MessageDTO;
import net.miarma.byodsec.common.db.entities.UserEntity;
import net.miarma.byodsec.client.ClientSocket;
import net.miginfocom.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jomaa
 */
public class MainWindow extends JFrame {
    public static UserEntity _loggedUser;
    private static final Logger logger = LoggerFactory.getLogger(ClientSocket.class);

    public MainWindow() {
        initComponents();
        this.setTitle(Constants.APP_NAME);
        versionLabel.setText(Constants.APP_NAME + " v" + Constants.APP_VERSION + " by Security Team 33");
        setIcons();
    }

    private void setIcons() {
        userLabel.setIcon(IconFontSwing.buildIcon(FontAwesome.USER, 18, Color.WHITE));
        passwordLabel.setIcon(IconFontSwing.buildIcon(FontAwesome.KEY, 18, Color.WHITE));
        loginBtn.setIcon(IconFontSwing.buildIcon(FontAwesome.SIGN_IN, 18, Color.WHITE));
        registerBtn.setIcon(IconFontSwing.buildIcon(FontAwesome.USER_PLUS, 18, Color.WHITE));
        clientLabel.setIcon(IconFontSwing.buildIcon(FontAwesome.USER_CIRCLE, 16, Color.WHITE));
        logoutBtn.setIcon(IconFontSwing.buildIcon(FontAwesome.SIGN_OUT, 16, Color.WHITE));
    }

    private void clearLoginInputs() {
        userTextField.setText("");
        passwordTextField.setText("");
    }

    private void clearMainInputs() {
        clientTextField.setText("");
    }

    private void appendChatMessage(String message) {
        chatTextArea.append(message + "\r\n");
        chatTextArea.setCaretPosition(chatTextArea.getDocument().getLength());
    }

    private void setView(String name) {
        ((CardLayout)getContentPane().getLayout()).show(getContentPane(), name);
    }

    private void loginBtn(ActionEvent e) {
        String username = userTextField.getText();
        String password = new String(passwordTextField.getPassword());

        try (ClientSocket client = new ClientSocket()) {
            SocketResponse res = client.login(username, password);

            if (res.getStatus() == SocketStatus.OK) {
                _loggedUser = new Gson().fromJson(
                        new Gson().toJson(res.getData()),
                        UserEntity.class
                );
                logger.info("El usuario '{}' ha iniciado sesión correctamente.", username);
                JOptionPane.showMessageDialog(this,
                        res.getMessage().getMessage(),
                        res.getStatus().toString(),
                        JOptionPane.INFORMATION_MESSAGE
                );
                clientLabel.setText("Cliente: " + _loggedUser.getUserName());
                this.setTitle(Constants.APP_NAME + " @" + _loggedUser.getUserName());
                clearLoginInputs();
                setView("mainPanel");
            } else if(res.getStatus() == SocketStatus.ALREADY_LOGGED_IN) {
                logger.info("El usuario '{}' ya tiene una sesión activa.", username);
                JOptionPane.showMessageDialog(this,
                        res.getMessage() != null ? res.getMessage().getMessage() : "Respuesta sin mensaje",
                        res.getStatus().toString(),
                        JOptionPane.WARNING_MESSAGE
                );
            } else {
                logger.warn("Intento de login fallido para '{}': {}", username,
                        res.getMessage() != null ? res.getMessage().getMessage() : "Respuesta sin mensaje");
                JOptionPane.showMessageDialog(this,
                        res.getMessage() != null ? res.getMessage().getMessage() : "Respuesta sin mensaje",
                        res.getStatus().toString(),
                        JOptionPane.ERROR_MESSAGE
                );
            }
        } catch (IOException ex) {
            logger.error("Error de conexión con el servidor al intentar loguear '{}': {}", username, ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Error de conexión con el servidor",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void registerBtn(ActionEvent e) {
        String username = userTextField.getText();
        String password = new String(passwordTextField.getPassword());

        try (ClientSocket client = new ClientSocket()) {
            SocketResponse res = client.register(username, password);

            if(res.getStatus() == SocketStatus.OK) {
                logger.info("Usuario '{}' registrado correctamente.", username);
            } else {
                logger.warn("Error al registrar '{}': {}", username,
                        res.getMessage() != null ? res.getMessage().getMessage() : "Respuesta sin mensaje");
            }

            JOptionPane.showMessageDialog(this,
                    res.getMessage() != null ? res.getMessage().getMessage() : "Respuesta sin mensaje",
                    res.getStatus().toString(),
                    res.getStatus() == SocketStatus.OK ?
                            JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE
            );

            clearLoginInputs();
        } catch (IOException ex) {
            logger.error("Error de conexión con el servidor al registrar '{}': {}", username, ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Error de conexión con el servidor",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void logout() {
        if (_loggedUser != null) {
            try (ClientSocket client = new ClientSocket()) {
                SocketResponse res = client.logout(_loggedUser.getUserId());

                if (res != null && res.getMessage() != null) {
                    if (res.getStatus() == SocketStatus.OK) {
                        logger.info("Usuario '{}' ha cerrado sesión correctamente.", _loggedUser.getUserName());
                        this.setTitle(Constants.APP_NAME);
                    } else {
                        logger.warn("Intento de logout de '{}' con estado: {}", _loggedUser.getUserName(), res.getStatus());
                    }

                    JOptionPane.showMessageDialog(this,
                            res.getMessage().getMessage(),
                            res.getStatus().toString(),
                            res.getStatus() == SocketStatus.OK ?
                                    JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE
                    );
                }
            } catch (IOException ex) {
                logger.error("Error de conexión con el servidor al hacer logout de '{}': {}", _loggedUser.getUserName(), ex.getMessage());
                JOptionPane.showMessageDialog(this,
                        "Error de conexión con el servidor",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }

        clearMainInputs();
        _loggedUser = null;
    }

    private void thisWindowClosing(WindowEvent e) {
        logout();
    }

    private void send(ActionEvent e) {
        try (ClientSocket client = new ClientSocket()) {
            String message = clientTextField.getText();
            MessageDTO dto = new MessageDTO(_loggedUser.getUserName(), message);
            SocketResponse res = client.sendMessage(dto);

            appendChatMessage(String.format("%s: %s", _loggedUser.getUserName(), message));
            appendChatMessage("Servidor: " + res.getData());

            if (res.getStatus() == SocketStatus.OK) {
                logger.info("Mensaje enviado: {} -> Servidor", _loggedUser.getUserName());
            } else if (res.getStatus() == SocketStatus.SESSION_EXPIRED) {
                logger.warn("Sesión expirada para '{}'. Se cerrará sesión.", _loggedUser.getUserName());
                _loggedUser = null;
                setView("loginPanel");
                clearMainInputs();
            } else {
                logger.warn("Error enviando mensaje de {} a Servidor: {}", _loggedUser.getUserName(),
                        res.getMessage() != null ? res.getMessage().getMessage() : "Sin mensaje");
            }

            clearMainInputs();
        } catch (Exception ex) {
            logger.error("Error de conexión con el servidor enviando mensaje de {} a Servidor: {}",
                    _loggedUser.getUserName(), ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Error de conexión con el servidor",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void logoutBtn(ActionEvent e) {
        logout();
        setView("loginPanel");
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner Educational license - José Manuel Amador Gallardo (José Manuel Amador)
        loginPanel = new JPanel();
        logo = new JLabel();
        userPanel = new JPanel();
        userLabel = new JLabel();
        userTextField = new JTextField();
        passwordPanel = new JPanel();
        passwordLabel = new JLabel();
        passwordTextField = new JPasswordField();
        loginBtn = new JButton();
        btnLoginPanel = new JPanel();
        noAccountLabel = new JLabel();
        registerBtn = new JButton();
        versionLabel = new JLabel();
        mainPanel = new JPanel();
        chatScrollPane = new JScrollPane();
        chatTextArea = new JTextArea();
        clientPanel = new JPanel();
        clientLabel = new JLabel();
        logoutBtn = new JButton();
        clientTextField = new JTextField();

        //======== this ========
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                thisWindowClosing(e);
            }
        });
        var contentPane = getContentPane();
        contentPane.setLayout(new CardLayout(6, 6));

        //======== loginPanel ========
        {
            loginPanel.setLayout(new MigLayout(
                "hidemode 3,gapy 12",
                // columns
                "[grow,fill]",
                // rows
                "[]para" +
                "[fill]" +
                "[fill]para" +
                "[]" +
                "[grow,fill]"));

            //---- logo ----
            logo.setIcon(new ImageIcon(getClass().getResource("/images/logo.png")));
            logo.setMaximumSize(new Dimension(256, 256));
            logo.setMinimumSize(new Dimension(256, 256));
            logo.setPreferredSize(new Dimension(256, 256));
            logo.setHorizontalTextPosition(SwingConstants.CENTER);
            loginPanel.add(logo, "cell 0 0,alignx center,growx 0");

            //======== userPanel ========
            {
                userPanel.setLayout(new MigLayout(
                    "insets 0,hidemode 3",
                    // columns
                    "[fill]" +
                    "[grow,fill]",
                    // rows
                    "[grow,fill]"));

                //---- userLabel ----
                userLabel.setText("Usuario:");
                userLabel.setFont(new Font("Adwaita Sans", Font.PLAIN, 18));
                userLabel.setMaximumSize(new Dimension(130, 23));
                userLabel.setMinimumSize(new Dimension(130, 23));
                userLabel.setPreferredSize(new Dimension(130, 23));
                userPanel.add(userLabel, "cell 0 0");

                //---- userTextField ----
                userTextField.setFont(new Font("Adwaita Sans", Font.PLAIN, 18));
                userPanel.add(userTextField, "cell 1 0");
            }
            loginPanel.add(userPanel, "cell 0 1");

            //======== passwordPanel ========
            {
                passwordPanel.setLayout(new MigLayout(
                    "insets 0,hidemode 3",
                    // columns
                    "[fill]" +
                    "[grow,fill]",
                    // rows
                    "[grow,fill]"));

                //---- passwordLabel ----
                passwordLabel.setText("Contrase\u00f1a:");
                passwordLabel.setFont(new Font("Adwaita Sans", Font.PLAIN, 18));
                passwordLabel.setMaximumSize(new Dimension(130, 23));
                passwordLabel.setMinimumSize(new Dimension(130, 23));
                passwordLabel.setPreferredSize(new Dimension(130, 23));
                passwordPanel.add(passwordLabel, "cell 0 0");

                //---- passwordTextField ----
                passwordTextField.setFont(new Font("Adwaita Sans", Font.PLAIN, 18));
                passwordPanel.add(passwordTextField, "cell 1 0");
            }
            loginPanel.add(passwordPanel, "cell 0 2");

            //---- loginBtn ----
            loginBtn.setText("Iniciar sesi\u00f3n");
            loginBtn.setFont(new Font("Adwaita Sans", Font.PLAIN, 18));
            loginBtn.addActionListener(e -> loginBtn(e));
            loginPanel.add(loginBtn, "cell 0 3,alignx trailing,growx 0");

            //======== btnLoginPanel ========
            {
                btnLoginPanel.setLayout(new MigLayout(
                    "hidemode 3,aligny bottom",
                    // columns
                    "[grow,fill]",
                    // rows
                    "[]" +
                    "[fill]para" +
                    "[]"));

                //---- noAccountLabel ----
                noAccountLabel.setText("\u00bfNo tiene cuenta?");
                btnLoginPanel.add(noAccountLabel, "cell 0 0,alignx center,growx 0");

                //---- registerBtn ----
                registerBtn.setText("Registrarse");
                registerBtn.setFont(new Font("Adwaita Sans", Font.PLAIN, 18));
                registerBtn.addActionListener(e -> registerBtn(e));
                btnLoginPanel.add(registerBtn, "cell 0 1,alignx center,growx 0");

                //---- versionLabel ----
                versionLabel.setForeground(new Color(0xa0a0a0));
                btnLoginPanel.add(versionLabel, "cell 0 2,alignx center,growx 0");
            }
            loginPanel.add(btnLoginPanel, "cell 0 4");
        }
        contentPane.add(loginPanel, "loginPanel");

        //======== mainPanel ========
        {
            mainPanel.setLayout(new MigLayout(
                "hidemode 3",
                // columns
                "[grow,fill]",
                // rows
                "[grow,fill]" +
                "[fill]" +
                "[fill]"));

            //======== chatScrollPane ========
            {

                //---- chatTextArea ----
                chatTextArea.setEditable(false);
                chatTextArea.setFocusable(false);
                chatTextArea.setFont(new Font("Adwaita Mono", Font.PLAIN, 16));
                chatScrollPane.setViewportView(chatTextArea);
            }
            mainPanel.add(chatScrollPane, "cell 0 0");

            //======== clientPanel ========
            {
                clientPanel.setLayout(new MigLayout(
                    "insets 0,hidemode 3",
                    // columns
                    "[grow,fill]" +
                    "[fill]",
                    // rows
                    "[]"));

                //---- clientLabel ----
                clientLabel.setText("Cliente: $c");
                clientLabel.setFont(new Font("Adwaita Sans", Font.PLAIN, 16));
                clientPanel.add(clientLabel, "cell 0 0");

                //---- logoutBtn ----
                logoutBtn.setText("Cerrar sesi\u00f3n");
                logoutBtn.setFont(new Font("Adwaita Sans", Font.PLAIN, 16));
                logoutBtn.addActionListener(e -> logoutBtn(e));
                clientPanel.add(logoutBtn, "cell 1 0");
            }
            mainPanel.add(clientPanel, "cell 0 1");

            //---- clientTextField ----
            clientTextField.addActionListener(e -> send(e));
            mainPanel.add(clientTextField, "cell 0 2");
        }
        contentPane.add(mainPanel, "mainPanel");
        setSize(400, 600);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner Educational license - José Manuel Amador Gallardo (José Manuel Amador)
    private JPanel loginPanel;
    private JLabel logo;
    private JPanel userPanel;
    private JLabel userLabel;
    private JTextField userTextField;
    private JPanel passwordPanel;
    private JLabel passwordLabel;
    private JPasswordField passwordTextField;
    private JButton loginBtn;
    private JPanel btnLoginPanel;
    private JLabel noAccountLabel;
    private JButton registerBtn;
    private JLabel versionLabel;
    private JPanel mainPanel;
    private JScrollPane chatScrollPane;
    private JTextArea chatTextArea;
    private JPanel clientPanel;
    private JLabel clientLabel;
    private JButton logoutBtn;
    private JTextField clientTextField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}