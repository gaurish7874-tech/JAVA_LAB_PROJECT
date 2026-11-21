package coderev.io;

import coderev.exception.PersistenceException;
import coderev.exception.ValidationException;
import coderev.model.ConfidenceLevel;
import coderev.model.Difficulty;
import coderev.model.Problem;
import coderev.util.AppLogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/** CSV export/import of the problem collection using character streams.
 *  Columns: title, platform, topic, difficulty, confidence, dateSolved,
 *  lastRevised, nextRevision, revisionCount, approachNotes, url */
public class CsvIO {
    private static final Logger LOG = AppLogger.getLogger(CsvIO.class);
    public static final String HEADER =
            "title,platform,topic,difficulty,confidence,dateSolved,lastRevised,nextRevision,revisionCount,approachNotes,url";

    public void exportProblems(List<Problem> problems, Path target) throws PersistenceException {
        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
        } catch (IOException e) {
            throw new PersistenceException("Could not create directory for " + target, e);
        }
        try (BufferedWriter w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            w.write(HEADER);
            w.newLine();
            for (Problem p : problems) {
                w.write(toCsvLine(p));
                w.newLine();
            }
            LOG.info("Exported " + problems.size() + " problems to " + target);
        } catch (IOException e) {
            throw new PersistenceException("Could not export CSV to " + target, e);
        }
    }

    /** Parses a CSV file back into problems. Every bad row is collected with
     *  its line number instead of aborting the whole import. */
    public ImportResult importProblems(Path source) throws PersistenceException {
        List<Problem> parsed = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = r.readLine()) != null) {
                lineNo++;
                if (lineNo == 1 && line.startsWith("title,")) {
                    continue; // header
                }
                if (line.isBlank()) {
                    continue;
                }
                try {
                    parsed.add(fromCsvLine(line));
                } catch (ValidationException | IllegalArgumentException e) {
                    errors.add("Line " + lineNo + ": " + e.getMessage());
                }
            }
            LOG.info("Imported " + parsed.size() + " problems from " + source
                    + " with " + errors.size() + " rejected rows");
            return new ImportResult(parsed, errors);
        } catch (IOException e) {
            throw new PersistenceException("Could not read CSV " + source, e);
        }
    }

    private static String toCsvLine(Problem p) {
        return String.join(",",
                escape(p.getTitle()), escape(p.getPlatform()), escape(p.getTopic()),
                p.getDifficulty().name(), String.valueOf(p.getConfidence().getLevel()),
                p.getDateSolved().toString(),
                p.getLastRevised() == null ? "" : p.getLastRevised().toString(),
                p.getNextRevision() == null ? "" : p.getNextRevision().toString(),
                String.valueOf(p.getRevisionCount()),
                escape(p.getApproachNotes()), escape(p.getUrl()));
    }

    private static Problem fromCsvLine(String line) throws ValidationException {
        List<String> cols = splitCsv(line);
        if (cols.size() < 11) {
            throw new ValidationException("expected 11 columns, found " + cols.size());
        }
        Problem p = new Problem();
        p.setTitle(required(cols.get(0), "title"));
        p.setPlatform(required(cols.get(1), "platform"));
        p.setTopic(required(cols.get(2), "topic"));
        p.setDifficulty(Difficulty.fromString(required(cols.get(3), "difficulty")));
        p.setConfidence(ConfidenceLevel.fromLevel(parseInt(cols.get(4), "confidence")));
        p.setDateSolved(LocalDate.parse(required(cols.get(5), "dateSolved")));
        p.setLastRevised(cols.get(6).isEmpty() ? null : LocalDate.parse(cols.get(6)));
        p.setNextRevision(cols.get(7).isEmpty() ? null : LocalDate.parse(cols.get(7)));
        p.setRevisionCount(parseInt(cols.get(8), "revisionCount"));
        p.setApproachNotes(cols.get(9));
        p.setUrl(cols.get(10));
        return p;
    }

    private static String required(String value, String field) throws ValidationException {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
        return value.trim();
    }

    private static int parseInt(String value, String field) throws ValidationException {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a number, got '" + value + "'");
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    /** Minimal CSV splitter handling quoted fields and escaped quotes. */
    private static List<String> splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    /** Result of an import: the parsed rows plus one message per rejected row. */
    public static final class ImportResult {
        private final List<Problem> problems;
        private final List<String> errors;

        ImportResult(List<Problem> problems, List<String> errors) {
            this.problems = problems;
            this.errors = errors;
        }

        public List<Problem> getProblems() { return problems; }
        public List<String> getErrors() { return errors; }
    }
}
