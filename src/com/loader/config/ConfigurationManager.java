package com.loader.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigurationManager {

    private final Properties properties;

    public ConfigurationManager(String configFilePath) throws IOException {
        properties = new Properties();
        try (FileInputStream input = new FileInputStream(Paths.get(configFilePath).toFile())) {
            properties.load(input);
        }
        resolveEnvironmentVariables();
    }

    private void resolveEnvironmentVariables() {
        Pattern pattern = Pattern.compile("\\$\\{(\\w+)\\}|\\$(\\w+)");

        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            Matcher matcher = pattern.matcher(value);
            StringBuffer buffer = new StringBuffer();

            while (matcher.find()) {
                String envVarName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                String envVarValue = System.getenv(envVarName);
                if (envVarValue == null) {
                    envVarValue = ""; // Replace with empty string if the environment variable is not defined
                }
                matcher.appendReplacement(buffer, envVarValue.replace("\\", "\\\\"));
            }
            matcher.appendTail(buffer);
            properties.setProperty(key, buffer.toString());
        }
    }
    
    public String getCSVCharset() {
        return properties.getProperty("CSV_CHARSET", "ISO-8859-1"); // Valor padrão ISO-8859-1
    }

    public String getProperty(String name) {
        return properties.getProperty(name);
    }
}
