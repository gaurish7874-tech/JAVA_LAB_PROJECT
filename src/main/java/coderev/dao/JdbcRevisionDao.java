package coderev.dao;

import coderev.exception.PersistenceException;
import coderev.model.ConfidenceLevel;
import coderev.model.RevisionRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** JDBC implementation of RevisionDao. */
public class JdbcRevisionDao implements RevisionDao {
    private final Database db;

    public JdbcRevisionDao(Database db) {
        this.db = db;
    }

    @Override
    public RevisionRecord insert(RevisionRecord r) throws PersistenceException {
        try (Connection conn = db.getConnection()) {
            return insert(conn, r);
        } catch (SQLException e) {
            throw new PersistenceException("Could not record revision for problem " + r.getProblemId(), e);
        }
    }

    /** Insert using a caller-managed connection, for use inside transactions. */
    public RevisionRecord insert(Connection conn, RevisionRecord r) throws SQLException {
        String sql = "INSERT INTO revision_history(problem_id, revised_at, previous_confidence,"
                + " new_confidence, revision_number, performance_percent) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, r.getProblemId());
            ps.setString(2, r.getRevisedAt().toString());
            if (r.getPreviousConfidence() == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, r.getPreviousConfidence().getLevel());
            }
            ps.setInt(4, r.getNewConfidence().getLevel());
            ps.setInt(5, r.getRevisionNumber());
            ps.setInt(6, r.getPerformancePercent());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    r.setId(keys.getLong(1));
                }
            }
            return r;
        }
    }

    @Override
    public List<RevisionRecord> findByProblemId(long problemId) throws PersistenceException {
        String sql = "SELECT * FROM revision_history WHERE problem_id = ? ORDER BY revision_number";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, problemId);
            try (ResultSet rs = ps.executeQuery()) {
                List<RevisionRecord> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not load revision history for problem " + problemId, e);
        }
    }

    @Override
    public List<RevisionRecord> findAll() throws PersistenceException {
        String sql = "SELECT * FROM revision_history ORDER BY revised_at";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<RevisionRecord> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapRow(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new PersistenceException("Could not load revision history", e);
        }
    }

    @Override
    public long count() throws PersistenceException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM revision_history");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new PersistenceException("Could not count revisions", e);
        }
    }

    private static RevisionRecord mapRow(ResultSet rs) throws SQLException {
        RevisionRecord r = new RevisionRecord();
        r.setId(rs.getLong("id"));
        r.setProblemId(rs.getLong("problem_id"));
        r.setRevisedAt(LocalDateTime.parse(rs.getString("revised_at")));
        int prev = rs.getInt("previous_confidence");
        r.setPreviousConfidence(rs.wasNull() ? null : ConfidenceLevel.fromLevel(prev));
        r.setNewConfidence(ConfidenceLevel.fromLevel(rs.getInt("new_confidence")));
        r.setRevisionNumber(rs.getInt("revision_number"));
        r.setPerformancePercent(rs.getInt("performance_percent"));
        return r;
    }
}
