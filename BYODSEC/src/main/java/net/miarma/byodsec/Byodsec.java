package net.miarma.byodsec;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.icons.google_material_design_icons.GoogleMaterialDesignIcons;
import jiconfont.swing.IconFontSwing;
import net.miarma.byodsec.common.ConfigManager;
import net.miarma.byodsec.common.db.DBPopulator;
import net.miarma.byodsec.client.ui.MainWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@SuppressWarnings("all")
public class Byodsec {
    private static final Logger logger = LoggerFactory.getLogger(Byodsec.class);
    private static ConfigManager configManager = ConfigManager.getInstance();

    public static void main(String[] args) {
        initConfig();
        initDB();
        initProperties();
        initJKS();
        initUI();
    }

    private static void initConfig() {
        File configFile = configManager.getConfigFile();
        File parentDir = configFile.getParentFile();

        if (!parentDir.exists()) {
            if (parentDir.mkdirs()) {
                logger.info("Created app directory: " + parentDir.getAbsolutePath());
            } else {
                logger.error("Failed to create app directory: " + parentDir.getAbsolutePath());
                return;
            }
        }

        if (!configFile.exists()) {
            try (InputStream in = Byodsec.class.getClassLoader().getResourceAsStream("default.properties")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Default config file created at: {}", configFile.getAbsolutePath());
                } else {
                    logger.error("Resource default.properties not found!");
                }
            } catch (IOException e) {
                logger.error("Error creating config file: ", e);
            }
        } else {
            logger.info("Config file already exists at: {}", configFile.getAbsolutePath());
        }

        configManager.loadConfig();
    }

    private static void initDB() {
        if(DBPopulator.getInstance().isFirstRun())
            DBPopulator.getInstance().populate();
    }

    private static void initJKS() {
        File jksFile = configManager.getJksFile();

        if (!jksFile.exists()) {
            String path = jksFile.getAbsolutePath();
            String password = configManager.getStringProperty("jksPassword");

            logger.info("Executing JKS initialization command...");
            ProcessBuilder pb = new ProcessBuilder(
                    "keytool",
                    "-genkeypair",
                    "-alias", "byodsec",
                    "-keyalg", "RSA",
                    "-keystore", path,
                    "-storepass", password,
                    "-keypass", password,
                    "-dname", "CN=localhost, OU=Byodsec, O=Chat33, L=Sevilla, ST=Andalucia, C=ES",
                    "-validity", "365"
            );

            pb.inheritIO();
            Process process = null;
            try {
                process = pb.start();
                process.waitFor();
            } catch (IOException e) {
                logger.error("Error executing JKS initialization command: ", e);
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                logger.error("Error executing JKS initialization command: ", e);
                throw new RuntimeException(e);
            }

        }
    }

    private static void initProperties() {
        // especificamos mediante propiedades el KS y la contraseña
        System.setProperty("javax.net.ssl.keyStore", configManager.getJksFile().getAbsolutePath());
        System.setProperty("javax.net.ssl.keyStorePassword", configManager.getStringProperty("jksPassword"));
        System.setProperty("javax.net.ssl.trustStore", configManager.getJksFile().getAbsolutePath());
        System.setProperty("javax.net.ssl.trustStorePassword", configManager.getStringProperty("jksPassword"));
    }

    private static void initUI() {
        try {
            UIManager.setLookAndFeel(new FlatMacDarkLaf());
        } catch(UnsupportedLookAndFeelException e) {
            logger.error("Error setting LaF. Falling back to default Swing looks.");
        }
        IconFontSwing.register(FontAwesome.getIconFont());
        IconFontSwing.register(GoogleMaterialDesignIcons.getIconFont());
        SwingUtilities.invokeLater(() -> {
            new MainWindow().setVisible(true);
        });
    }
}