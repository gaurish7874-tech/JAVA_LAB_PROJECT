package coderev.dao;

import coderev.exception.PersistenceException;
import coderev.model.ConfidenceLevel;
import coderev.model.Difficulty;
import coderev.model.Problem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of ProblemDao. Every read/write uses PreparedStatement. */
public class JdbcProblemDao implements ProblemDao {
    private final Database db;

    public JdbcProblemDao(Database db) {
        this.db = db;
    }

    @Override
    public Problem insert(Problem p) throws PersistenceException {
        String sql = "INSERT INTO problems(title, platform, topic, difficulty, confidence,"
                + " date_solved, last_revised, next_revision, revision_count, approach_notes, url)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, p);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setId(keys.getLong(1));
                }
            }
            return p;
        } catch (SQLException e) {
            throw new PersistenceException("Could not insert problem '" + p.getTitle() + "'", e);
        }
    }

    /** Insert using a caller-managed connection, for use inside transactions. */
    public Problem insert(Connection conn, Problem p) throws SQLException {
        String sql = "INSERT INTO problems(title, platform, topic, difficulty, confidence,"
                + " date_solved, last_revised, next_revision, revision_count, approach_notes, url)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, p);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setId(keys.getLong(1));
                }
            }
            return p;
        }
    }

    private static void bind(PreparedStatement ps, Problem p) throws SQLException {
        ps.setString(1, p.getTitle());
        ps.setString(2, p.getPlatform());
        ps.setString(3, p.getTopic());
        ps.setString(4, p.getDifficulty().name());
        ps.setInt(5, p.getConfidence().getLevel());
        ps.setString(6, p.getDateSolved().toString());
        ps.setString(7, p.getLastRevised() == null ? null : p.getLastRevised().toString());
        ps.setString(8, p.getNextRevision() == null ? null : p.getNextRevision().toString());
        ps.setInt(9, p.getRevisionCount());
        ps.setString(10, p.getApproachNotes() == null ? "" : p.getApproachNotes());
        ps.setString(11, p.getUrl() == null ? "" : p.getUrl());
    }

    @Override
    public Optional<Problem> findById(Long id) throws PersistenceException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM problems WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not load problem " + id, e);
        }
    }

    @Override
    public Optional<Problem> findByTitleAndPlatform(String title, String platform) throws PersistenceException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM problems WHERE lower(title) = lower(?) AND lower(platform) = lower(?)")) {
            ps.setString(1, title);
            ps.setString(2, platform);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not look up problem '" + title + "'", e);
        }
    }

    @Override
    public List<Problem> findAll() throws PersistenceException {
        return query("SELECT * FROM problems ORDER BY title");
    }

    @Override
    public List<Problem> findDueOnOrBefore(LocalDate date) throws PersistenceException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM problems WHERE next_revision IS NOT NULL AND next_revision <= ?"
                             + " ORDER BY next_revision")) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                List<Problem> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not load problems due by " + date, e);
        }
    }

    @Override
    public List<Problem> search(String topic, Difficulty difficulty, Integer confidenceLevel,
                                String titleContains) throws PersistenceException {
        StringBuilder sql = new StringBuilder("SELECT * FROM problems WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (topic != null && !topic.isBlank()) {
            sql.append(" AND topic = ?");
            params.add(topic);
        }
        if (difficulty != null) {
            sql.append(" AND difficulty = ?");
            params.add(difficulty.name());
        }
        if (confidenceLevel != null) {
            sql.append(" AND confidence = ?");
            params.add(confidenceLevel);
        }
        if (titleContains != null && !titleContains.isBlank()) {
            sql.append(" AND lower(title) LIKE ?");
            params.add("%" + titleContains.toLowerCase() + "%");
        }
        sql.append(" ORDER BY title");
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Problem> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Search failed", e);
        }
    }

    @Override
    public List<String> findDistinctTopics() throws PersistenceException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT DISTINCT topic FROM problems ORDER BY topic");
             ResultSet rs = ps.executeQuery()) {
            List<String> out = new ArrayList<>();
            while (rs.next()) {
                out.add(rs.getString(1));
            }
            return out;
        } catch (SQLException e) {
            throw new PersistenceException("Could not load topics", e);
        }
    }

    @Override
    public void update(Problem p) throws PersistenceException {
        String sql = "UPDATE problems SET title=?, platform=?, topic=?, difficulty=?, confidence=?,"
                + " date_solved=?, last_revised=?, next_revision=?, revision_count=?,"
                + " approach_notes=?, url=? WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, p);
            ps.setLong(12, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Could not update problem " + p.getId(), e);
        }
    }

    /** Update using a caller-managed connection, for use inside transactions. */
    public void update(Connection conn, Problem p) throws SQLException {
        String sql = "UPDATE problems SET title=?, platform=?, topic=?, difficulty=?, confidence=?,"
                + " date_solved=?, last_revised=?, next_revision=?, revision_count=?,"
                + " approach_notes=?, url=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, p);
            ps.setLong(12, p.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Long id) throws PersistenceException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM problems WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Could not delete problem " + id, e);
        }
    }

    @Override
    public long count() throws PersistenceException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM problems");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new PersistenceException("Could not count problems", e);
        }
    }

    private List<Problem> query(String sql) throws PersistenceException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Problem> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapRow(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new PersistenceException("Query failed: " + sql, e);
        }
    }

    static Problem mapRow(ResultSet rs) throws SQLException {
        Problem p = new Problem();
        p.setId(rs.getLong("id"));
        p.setTitle(rs.getString("title"));
        p.setPlatform(rs.getString("platform"));
        p.setTopic(rs.getString("topic"));
        p.setDifficulty(Difficulty.fromString(rs.getString("difficulty")));
        p.setConfidence(ConfidenceLevel.fromLevel(rs.getInt("confidence")));
        p.setDateSolved(LocalDate.parse(rs.getString("date_solved")));
        String lastRevised = rs.getString("last_revised");
        p.setLastRevised(lastRevised == null ? null : LocalDate.parse(lastRevised));
        String nextRevision = rs.getString("next_revision");
        p.setNextRevision(nextRevision == null ? null : LocalDate.parse(nextRevision));
        p.setRevisionCount(rs.getInt("revision_count"));
        p.setApproachNotes(rs.getString("approach_notes"));
        p.setUrl(rs.getString("url"));
        return p;
    }
}
