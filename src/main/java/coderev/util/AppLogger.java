package coderev.util;

import coderev.io.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/** Central java.util.logging setup: one log file plus console output. */
public final class AppLogger {
    private static volatile boolean initialised = false;

    private AppLogger() {
    }

    public static synchronized void init(AppConfig config) {
        if (initialised) {
            return;
        }
        try {
            if (config.logFile().getParent() != null) {
                Files.createDirectories(config.logFile().getParent());
            }
            Logger root = Logger.getLogger("coderev");
            root.setUseParentHandlers(false);
            Level level = Level.parse(config.logLevel());
            root.setLevel(level);

            FileHandler fileHandler = new FileHandler(config.logFile().toString(), true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(level);
            root.addHandler(fileHandler);

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.WARNING);
            root.addHandler(consoleHandler);

            initialised = true;
            root.info("Logging initialised at level " + level);
        } catch (IOException e) {
            // Logging must never crash the app; fall back to console only.
            System.err.println("Could not open log file: " + e.getMessage());
        }
    }

    public static Logger getLogger(Class<?> type) {
        return Logger.getLogger("coderev." + type.getSimpleName());
    }
}
