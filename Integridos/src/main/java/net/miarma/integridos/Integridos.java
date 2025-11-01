package net.miarma.integridos;

import com.formdev.flatlaf.themes.FlatMacLightLaf;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.icons.google_material_design_icons.GoogleMaterialDesignIcons;
import jiconfont.swing.IconFontSwing;
import net.miarma.integridos.common.ConfigManager;
import net.miarma.integridos.common.Constants;
import net.miarma.integridos.common.db.DBPopulator;
import net.miarma.integridos.client.ui.MainWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Integridos {
    private static final Logger logger = LoggerFactory.getLogger(Integridos.class);
    private static ConfigManager configManager;

    public static void main(String[] args) {
        configManager = ConfigManager.getInstance();
        createFiles();
        configManager.loadConfig();
        if(DBPopulator.getInstance().isFirstRun()) {
            DBPopulator.getInstance().populate();
        }
        initUI();
    }

    private static void initUI() {
        try {
            UIManager.setLookAndFeel(new FlatMacLightLaf());
        } catch(UnsupportedLookAndFeelException e) {
            logger.error("Error setting LaF. Falling back to default Swing looks.");
        }
        IconFontSwing.register(FontAwesome.getIconFont());
        IconFontSwing.register(GoogleMaterialDesignIcons.getIconFont());
        SwingUtilities.invokeLater(() -> {
            new MainWindow().setVisible(true);
        });
    }

    private static void createFiles() {
        File configFile = new File(configManager.getConfigFile().getAbsolutePath());
        File parentDir = configFile.getParentFile();

        if (!parentDir.exists()) {
            if (parentDir.mkdirs()) {
                logger.info("Created config directory: " + parentDir.getAbsolutePath());
            } else {
                logger.error("Failed to create config directory: " + parentDir.getAbsolutePath());
                return;
            }
        }

        if (!configFile.exists()) {
            try (InputStream in = Integridos.class.getClassLoader().getResourceAsStream("default.properties")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Default config file created at: " + configFile.getAbsolutePath());
                } else {
                    logger.error("Resource default.properties not found!");
                }
            } catch (IOException e) {
                logger.error("Error creating config file: ", e);
            }
        } else {
            logger.info("Config file already exists at: " + configFile.getAbsolutePath());
        }
    }


}