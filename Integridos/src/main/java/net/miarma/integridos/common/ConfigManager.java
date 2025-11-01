package net.miarma.integridos.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Gestión de toda la configuración de la aplicación.
 * Se encarga de cargar, guardar y proporcionar acceso a las propiedades de configuración.
 * Proporciona métodos para obtener la URL de la base de datos, directorios de archivos,
 * y propiedades específicas como host, puerto, etc.
 * <p>
 * Esta clase sigue el patron Singleton para asegurar una sola instancia.
 * @author José Manuel Amador Gallardo
 */
public class ConfigManager {
    private static ConfigManager INSTANCE;
    private final File configFile;
    private final Properties config;
    private static final String CONFIG_FILE_NAME = "config.properties";
    private final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    private ConfigManager() {
        String path = getBaseDir() + CONFIG_FILE_NAME;
        this.configFile = new File(path);
        this.config = new Properties();
    }

    public static ConfigManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ConfigManager();
        }
        return INSTANCE;
    }

    public void loadConfig() {
        try (FileInputStream fis = new FileInputStream(configFile);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            config.load(isr);
        } catch (IOException e) {
            logger.error("Error loading configuration file: ", e);
        }
    }
    public File getConfigFile() {
        return configFile;
    }

    public String getHomeDir() {
        return getOS() == OSType.WINDOWS ?
                "C:/Users/" + System.getProperty("user.name") + "/" :
                System.getProperty("user.home").contains("root") ? "/root/" :
                        "/home/" + System.getProperty("user.name") + "/";
    }

    public String getBaseDir() {
        return getHomeDir() +
                (getOS() == OSType.WINDOWS ? ".integridos/" :
                        getOS() == OSType.LINUX ? ".config/integridos/" :
                                ".integridos/");
    }

    public static OSType getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return OSType.WINDOWS;
        } else if (os.contains("nix") || os.contains("nux")) {
            return OSType.LINUX;
        } else {
            return OSType.INVALID_OS;
        }
    }

    public String getStringProperty(String key) {
        return config.getProperty(key);
    }

    public int getIntProperty(String key) {
        String value = config.getProperty(key);
        return value != null ? Integer.parseInt(value) : 10;
    }

    public boolean getBooleanProperty(String key) {
        return Boolean.parseBoolean(config.getProperty(key));
    }

    public void setProperty(String key, String value) {
        config.setProperty(key, value);
        saveConfig();
    }

    private void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            config.store(fos, "Configuration for: " + Constants.APP_NAME);
        } catch (IOException e) {
            logger.error("Error saving configuration file: ", e);
        }
    }
}