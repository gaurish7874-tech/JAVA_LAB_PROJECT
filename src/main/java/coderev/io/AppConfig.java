package coderev.io;

import coderev.exception.PersistenceException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/** Loads config/coderev.properties once and hands out typed settings with
 *  safe defaults, so the app still runs when the file is missing. */
public final class AppConfig {
    public static final String DEFAULT_PATH = "config/coderev.properties";

    private final Properties props = new Properties();

    private AppConfig() {
        props.setProperty("db.path", "data/coderev.db");
        props.setProperty("log.file", "logs/coderev.log");
        props.setProperty("log.level", "INFO");
        props.setProperty("weight.confidence", "2.0");
        props.setProperty("weight.difficulty", "1.5");
        props.setProperty("weight.recency", "0.5");
        props.setProperty("weight.frequency", "0.3");
    }

    public static AppConfig load() throws PersistenceException {
        return load(DEFAULT_PATH);
    }

    public static AppConfig load(String path) throws PersistenceException {
        AppConfig config = new AppConfig();
        Path file = Paths.get(path);
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                config.props.load(in);
            } catch (IOException e) {
                throw new PersistenceException("Could not read config file " + path, e);
            }
        }
        return config;
    }

    public String get(String key) {
        return props.getProperty(key);
    }

    public double getDouble(String key) {
        return Double.parseDouble(props.getProperty(key));
    }

    public Path dbPath() {
        return Paths.get(props.getProperty("db.path"));
    }

    public Path logFile() {
        return Paths.get(props.getProperty("log.file"));
    }

    public String logLevel() {
        return props.getProperty("log.level");
    }
}
