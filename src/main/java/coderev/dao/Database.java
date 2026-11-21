package coderev.dao;

import coderev.exception.PersistenceException;
import coderev.util.AppLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/** Owns the SQLite connection details, creates the schema on first use and
 *  provides a transaction helper so services never touch JDBC plumbing. */
public class Database {
    private static final Logger LOG = AppLogger.getLogger(Database.class);

    private final String jdbcUrl;

    public Database(Path dbFile) throws PersistenceException {
        try {
            if (dbFile.getParent() != null) {
                Files.createDirectories(dbFile.getParent());
            }
            this.jdbcUrl = "jdbc:sqlite:" + dbFile.toAbsolutePath();
            initialize();
        } catch (java.io.IOException e) {
            throw new PersistenceException("Could not create database directory for " + dbFile, e);
        }
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    private void initialize() throws PersistenceException {
        String schema =
                "CREATE TABLE IF NOT EXISTS problems ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " title TEXT NOT NULL,"
                + " platform TEXT NOT NULL,"
                + " topic TEXT NOT NULL,"
                + " difficulty TEXT NOT NULL,"
                + " confidence INTEGER NOT NULL,"
                + " date_solved TEXT NOT NULL,"
                + " last_revised TEXT,"
                + " next_revision TEXT,"
                + " revision_count INTEGER NOT NULL DEFAULT 0,"
                + " approach_notes TEXT DEFAULT '',"
                + " url TEXT DEFAULT '',"
                + " UNIQUE(title, platform)"
                + ");"
                + "CREATE TABLE IF NOT EXISTS revision_history ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " problem_id INTEGER NOT NULL REFERENCES problems(id) ON DELETE CASCADE,"
                + " revised_at TEXT NOT NULL,"
                + " previous_confidence INTEGER,"
                + " new_confidence INTEGER NOT NULL,"
                + " revision_number INTEGER NOT NULL,"
                + " performance_percent INTEGER NOT NULL DEFAULT 0"
                + ");";
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            for (String ddl : schema.split(";")) {
                if (!ddl.trim().isEmpty()) {
                    st.execute(ddl);
                }
            }
            LOG.info("Database schema ready at " + jdbcUrl);
        } catch (SQLException e) {
            throw new PersistenceException("Could not initialise database schema", e);
        }
    }

    /** Runs the body inside one transaction: commit on success, rollback on any failure. */
    public <T> T inTransaction(TransactionBody<T> body) throws PersistenceException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = body.run(conn);
                conn.commit();
                return result;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof PersistenceException) {
                    throw (PersistenceException) e;
                }
                throw new PersistenceException("Transaction failed and was rolled back", e);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not open a database transaction", e);
        }
    }

    @FunctionalInterface
    public interface TransactionBody<T> {
        T run(Connection conn) throws Exception;
    }
}
