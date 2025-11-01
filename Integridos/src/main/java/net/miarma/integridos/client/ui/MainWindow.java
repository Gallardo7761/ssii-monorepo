/*
 * Created by JFormDesigner on Wed Oct 01 16:21:10 CEST 2025
 */

package net.miarma.integridos.client.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.*;

import com.google.gson.Gson;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import net.miarma.integridos.common.Constants;

import net.miarma.integridos.common.security.IntegrityProvider;
import net.miarma.integridos.common.socket.SocketResponse;
import net.miarma.integridos.common.socket.SocketStatus;
import net.miarma.integridos.common.db.dto.TransactionDTO;
import net.miarma.integridos.common.db.entities.UserEntity;
import net.miarma.integridos.client.ClientSocket;
import net.miginfocom.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jomaa
 */
public class MainWindow extends JFrame {
    public static UserEntity _loggedUser;
    private AutoShrinkLabel balanceLabel = new AutoShrinkLabel("0.00€");
    private static final Logger logger = LoggerFactory.getLogger(ClientSocket.class);

    public MainWindow() {
        initComponents();
        this.setTitle(Constants.APP_NAME);
        versionLabel.setText(Constants.APP_NAME + " v" + Constants.APP_VERSION + " by Security Team 33");
        setIcons();
        addBalanceLabel();
    }

    private void setIcons() {
        userLabel.setIcon(IconFontSwing.buildIcon(FontAwesome.USER, 18, Color.BLACK));
        passwordLabel.setIcon(IconFontSwing.buildIcon(FontAwesome.KEY, 18, Color.BLACK));
        loginBtn.setIcon(IconFontSwing.buildIcon(FontAwesome.SIGN_IN, 18, Color.BLACK));
        registerBtn.setIcon(IconFontSwing.buildIcon(FontAwesome.USER_PLUS, 18, Color.BLACK));
        destLabel.setIcon(IconFontSwing.buildIcon(FontAwesome.USER_SECRET, 18, Color.BLACK));
        amountLabel.setIcon(IconFontSwing.buildIcon(FontAwesome.MONEY, 18, Color.BLACK));
    }

    private void clearLoginInputs() {
        userTextField.setText("");
        passwordTextField.setText("");
    }

    private void clearMainInputs() {
        destTextField.setText("");
        amountTextField.setText("");
    }

    private void addBalanceLabel() {
        balanceLabel.setFont(new Font("Adwaita Sans", Font.BOLD, 64));
        balanceLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(balanceLabel, "cell 0 0,alignx center,growx 0");
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
                setView("mainPanel");
                balanceLabel.setText(String.format("%.2f€", _loggedUser.getBalance()));
            } else {
                logger.warn("Intento de login fallido para '{}': {}", username,
                        res.getMessage() != null ? res.getMessage().getMessage() : "Respuesta sin mensaje");
                JOptionPane.showMessageDialog(this,
                        res.getMessage() != null ? res.getMessage().getMessage() : "Respuesta sin mensaje",
                        res.getStatus().toString(),
                        JOptionPane.ERROR_MESSAGE
                );
            }

            clearLoginInputs();
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

    private void sendBtn(ActionEvent e) {
        String destUser = destTextField.getText();
        double amount;

        try {
            amount = Double.parseDouble(amountTextField.getText());
        } catch (NumberFormatException ex) {
            logger.warn("Cantidad no válida introducida: {}", amountTextField.getText());
            JOptionPane.showMessageDialog(this,
                    "Cantidad no válida.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        try (ClientSocket client = new ClientSocket()) {
            String nonce = IntegrityProvider.generateNonce();
            TransactionDTO dto = new TransactionDTO(_loggedUser.getUserName(), destUser, amount, nonce);
            SocketResponse res = client.sendTransaction(dto);

            JOptionPane.showMessageDialog(this,
                    res.getMessage() != null ? res.getMessage().getMessage() : "Respuesta sin mensaje",
                    res.getStatus().toString(),
                    res.getStatus() == SocketStatus.OK ?
                            JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE
            );

            if (res.getStatus() == SocketStatus.OK) {
                logger.info("Transacción enviada: {} -> {} | {}€", _loggedUser.getUserName(), destUser, amount);
                _loggedUser.setBalance(_loggedUser.getBalance() - amount);
                balanceLabel.setText(String.format("%.2f€", _loggedUser.getBalance()));
            } else if (res.getStatus() == SocketStatus.SESSION_EXPIRED) {
                logger.warn("Sesión expirada para '{}'. Se cerrará sesión.", _loggedUser.getUserName());
                _loggedUser = null;
                setView("loginPanel");
                clearMainInputs();
            } else {
                logger.warn("Error enviando transacción de {} a {}: {}", _loggedUser.getUserName(), destUser,
                        res.getMessage() != null ? res.getMessage().getMessage() : "Sin mensaje");
            }

            clearMainInputs();
        } catch (Exception ex) {
            logger.error("Error de conexión con el servidor enviando transacción de {} a {}: {}",
                    _loggedUser.getUserName(), destUser, ex.getMessage());
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

    private void logoutBtn(ActionEvent e) {
        logout();
        setView("loginPanel");
    }

    private void thisWindowClosing(WindowEvent e) {
        logout();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner Educational license - José Manuel Amador Gallardo (José Manuel Amador)
        loginPanel = new JPanel();
        logo = new JLabel();
        welcomeLabel = new JLabel();
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
        destPanel = new JPanel();
        destLabel = new JLabel();
        destTextField = new JTextField();
        amountPanel = new JPanel();
        amountLabel = new JLabel();
        amountTextField = new JTextField();
        sendBtn = new JButton();
        logoutBtn = new JButton();

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
        contentPane.setLayout(new CardLayout(12, 6));

        //======== loginPanel ========
        {
            loginPanel.setLayout(new MigLayout(
                "hidemode 3,gapy 12",
                // columns
                "[grow,fill]",
                // rows
                "[]0" +
                "[]para" +
                "[fill]" +
                "[fill]para" +
                "[]" +
                "[grow,fill]"));

            //---- logo ----
            logo.setIcon(new ImageIcon(getClass().getResource("/images/banco.png")));
            logo.setMaximumSize(new Dimension(256, 256));
            logo.setMinimumSize(new Dimension(256, 256));
            logo.setPreferredSize(new Dimension(256, 256));
            logo.setHorizontalTextPosition(SwingConstants.CENTER);
            loginPanel.add(logo, "cell 0 0,alignx center,growx 0");

            //---- welcomeLabel ----
            welcomeLabel.setText("Bienvenido a Banco Grupo 33. Inicie sesi\u00f3n.");
            welcomeLabel.setFont(new Font("Adwaita Sans", Font.PLAIN, 16));
            loginPanel.add(welcomeLabel, "cell 0 1,alignx center,growx 0");

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
                userTextField.setCaretColor(new Color(0x076854));
                userTextField.setSelectionColor(new Color(0x076854));
                userPanel.add(userTextField, "cell 1 0");
            }
            loginPanel.add(userPanel, "cell 0 2");

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
                passwordTextField.setSelectionColor(new Color(0x076854));
                passwordTextField.setCaretColor(new Color(0x076854));
                passwordPanel.add(passwordTextField, "cell 1 0");
            }
            loginPanel.add(passwordPanel, "cell 0 3");

            //---- loginBtn ----
            loginBtn.setText("Iniciar sesi\u00f3n");
            loginBtn.setFont(new Font("Adwaita Sans", Font.PLAIN, 18));
            loginBtn.addActionListener(e -> loginBtn(e));
            loginPanel.add(loginBtn, "cell 0 4,alignx trailing,growx 0");

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
            loginPanel.add(btnLoginPanel, "cell 0 5");
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
                "[fill]" +
                "[fill]" +
                "[grow,fill]"));

            //======== destPanel ========
            {
                destPanel.setLayout(new MigLayout(
                    "insets 0,hidemode 3",
                    // columns
                    "[fill]" +
                    "[grow,fill]",
                    // rows
                    "[]"));

                //---- destLabel ----
                destLabel.setText("Destinatario:");
                destLabel.setFont(new Font("Adwaita Sans", Font.PLAIN, 18));
                destLabel.setHorizontalAlignment(SwingConstants.LEFT);
                destPanel.add(destLabel, "cell 0 0");
                destPanel.add(destTextField, "cell 1 0");
            }
            mainPanel.add(destPanel, "cell 0 1");

            //======== amountPanel ========
            {
                amountPanel.setLayout(new MigLayout(
                    "insets 0,hidemode 3",
                    // columns
                    "[fill]" +
                    "[grow,fill]",
                    // rows
                    "[grow,fill]"));

                //---- amountLabel ----
                amountLabel.setText("Cantidad:");
                amountLabel.setFont(new Font("Adwaita Sans", Font.PLAIN, 18));
                amountLabel.setMaximumSize(new Dimension(108, 23));
                amountLabel.setMinimumSize(new Dimension(108, 23));
                amountLabel.setPreferredSize(new Dimension(108, 23));
                amountLabel.setHorizontalAlignment(SwingConstants.LEFT);
                amountPanel.add(amountLabel, "cell 0 0");
                amountPanel.add(amountTextField, "cell 1 0");
            }
            mainPanel.add(amountPanel, "cell 0 2");

            //---- sendBtn ----
            sendBtn.setText("Enviar");
            sendBtn.setFont(new Font("Adwaita Sans", Font.PLAIN, 18));
            sendBtn.addActionListener(e -> sendBtn(e));
            mainPanel.add(sendBtn, "cell 0 3,alignx trailing,growx 0");

            //---- logoutBtn ----
            logoutBtn.setText("Cerrar sesi\u00f3n");
            logoutBtn.setFont(new Font("Adwaita Sans", Font.PLAIN, 18));
            logoutBtn.addActionListener(e -> logoutBtn(e));
            mainPanel.add(logoutBtn, "cell 0 4,aligny bottom,growy 0");
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
    private JLabel welcomeLabel;
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
    private JPanel destPanel;
    private JLabel destLabel;
    private JTextField destTextField;
    private JPanel amountPanel;
    private JLabel amountLabel;
    private JTextField amountTextField;
    private JButton sendBtn;
    private JButton logoutBtn;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}