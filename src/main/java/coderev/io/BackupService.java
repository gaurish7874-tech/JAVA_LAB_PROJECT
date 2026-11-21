package coderev.io;

import coderev.exception.PersistenceException;
import coderev.model.Problem;
import coderev.model.RevisionRecord;
import coderev.util.AppLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

/** Full-collection backup and restore through Java object serialization. */
public class BackupService {
    private static final Logger LOG = AppLogger.getLogger(BackupService.class);

    public void backup(List<Problem> problems, List<RevisionRecord> revisions, Path target)
            throws PersistenceException {
        BackupSnapshot snapshot = new BackupSnapshot(problems, revisions);
        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
        } catch (IOException e) {
            throw new PersistenceException("Could not create directory for " + target, e);
        }
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(target))) {
            out.writeObject(snapshot);
            LOG.info("Backup written to " + target + " (" + problems.size() + " problems, "
                    + revisions.size() + " revisions)");
        } catch (IOException e) {
            throw new PersistenceException("Could not write backup " + target, e);
        }
    }

    @SuppressWarnings("unchecked")
    public BackupSnapshot restore(Path source) throws PersistenceException {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(source))) {
            Object obj = in.readObject();
            if (!(obj instanceof BackupSnapshot)) {
                throw new PersistenceException(source + " is not a CodeRev backup file");
            }
            BackupSnapshot snapshot = (BackupSnapshot) obj;
            LOG.info("Backup restored from " + source + " (" + snapshot.getProblems().size()
                    + " problems, " + snapshot.getRevisions().size() + " revisions)");
            return snapshot;
        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenceException("Could not restore backup " + source, e);
        }
    }

    /** Serializable container for one backup file. */
    public static final class BackupSnapshot implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private final List<Problem> problems;
        private final List<RevisionRecord> revisions;

        BackupSnapshot(List<Problem> problems, List<RevisionRecord> revisions) {
            this.problems = problems;
            this.revisions = revisions;
        }

        public List<Problem> getProblems() { return problems; }
        public List<RevisionRecord> getRevisions() { return revisions; }
    }
}
