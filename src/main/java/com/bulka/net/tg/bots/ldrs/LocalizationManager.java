package com.bulka.net.tg.bots.ldrs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public class LocalizationManager {
    private static final Logger LOGGER = Logger.getLogger(LocalizationManager.class.getName());

    private ResourceBundle bundleEN;
    private ResourceBundle bundleUK;
    private static final UTF8Control UTF_8_CONTROL = new UTF8Control();

    public LocalizationManager() {

    }

    public void load() {
        bundleEN = ResourceBundle.getBundle("locale_en", UTF_8_CONTROL);
        bundleUK = ResourceBundle.getBundle("locale_uk", UTF_8_CONTROL);
    }


    public String getString(String lang, String key) {
        try {
            switch (lang){
                case "uk":
                    return bundleUK.getString(key);
                default:
                    return bundleEN.getString(key);
            }
        } catch (Exception e) {
            try {
                return bundleEN.getString(key);
            } catch (Exception ex) {
                return key;
            }
        }
    }

    private static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    java.net.URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try (InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    bundle = new PropertyResourceBundle(isr);
                }
            }
            return bundle;
        }
    }
}
