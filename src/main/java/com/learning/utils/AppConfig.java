package com.learning.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
@UtilityClass
public class AppConfig {

    private static final Properties PROPERTIES = new Properties();
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    static {
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                log.warn("application.properties not found on classpath, defaulting to empty");
            } else {
                PROPERTIES.load(input);
                log.info("Loaded application.properties successfully");
            }
        } catch (IOException ex) {
            log.error("Failed to load application.properties", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static String get(String key) {
        return PROPERTIES.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String val = PROPERTIES.getProperty(key);
        return val != null ? Integer.parseInt(val.trim()) : defaultValue;
    }

    public static double getDouble(String key, double defaultValue) {
        String val = PROPERTIES.getProperty(key);
        return val != null ? Double.parseDouble(val.trim()) : defaultValue;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String val = PROPERTIES.getProperty(key);
        return val != null ? Boolean.parseBoolean(val.trim()) : defaultValue;
    }

    /**
     * Resolves API key from application.properties, falling back to system environment variables.
     */
    public static String getApiKey() {
        String key = get("api.key");
        if (key == null || key.isEmpty()) {
            key = System.getenv("API_KEY");
        }
        return key;
    }
}