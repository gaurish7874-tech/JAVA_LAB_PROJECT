# CodeRev — Smart LeetCode Revision System

CodeRev is a desktop application that helps students keep what they solved. You record
each solved LeetCode-style problem with its topic, difficulty, confidence and approach
notes, and CodeRev builds a personalized, explainable revision schedule from difficulty,
confidence, revision recency, revision frequency and topic-wise weakness.

Built in Core Java for the Java lab project (team GSK, group 3C33).

## Features

- **Problem management** — add, search, filter and delete solved problems
  (title, platform, topic, difficulty, confidence, approach notes, URL).
- **Deterministic revision scheduling** — the next revision date is computed as
  `baseInterval(confidence) x difficultyMultiplier x growth(pastRevisions)`,
  clamped to 1–60 days. Same state, same schedule: transparent and reproducible.
- **Daily recommendation** — every due problem gets a priority score
  `wc x confidenceNeed + wd x difficulty + wr x recencyDays + wf x revisionCount`
  (weights in `config/coderev.properties`), and the app shows the top pick with a
  full breakdown of *why* it was chosen.
- **Revision history** — every revision stores the confidence change, a performance
  percentage and its sequence number, written with the schedule update in one
  database transaction.
- **Analytics** — totals, due counts, per-difficulty and per-confidence distributions,
  per-topic averages and the weakest topic.
- **Data tools** — CSV export/import and full binary backup/restore via Java
  serialization; activity log written with `java.util.logging`.

## Architecture

Layered design with the DAO / Service / UI separation:

```
coderev
├── model       Problem, RevisionRecord, Difficulty, ConfidenceLevel (Serializable POJOs)
├── exception   CodeRevException, ValidationException, DuplicateProblemException,
│               ProblemNotFoundException, PersistenceException
├── dao         Database (SQLite + transactions), GenericDao<T,ID>,
│               ProblemDao / JdbcProblemDao, RevisionDao / JdbcRevisionDao
├── service     RevisionScheduler / SpacedRepetitionScheduler, PriorityScorer,
│               ProblemService, RevisionService, AnalyticsService
├── io          AppConfig (properties), CsvIO (character streams), BackupService (serialization)
├── util        AppLogger (java.util.logging), Validators
├── console     ConsoleApp (menu-driven front end)
└── ui          CodeRevApp, ProblemFormDialog (JavaFX front end)
```

## Tech stack

- Java 11+, SQLite via JDBC (`sqlite-jdbc`), JavaFX (OpenJFX 17)
- Maven (`pom.xml`) or plain `javac` (`build.sh` / `run.sh`)

## Build and run

With Maven:

```bash
mvn compile
mvn exec:java            # JavaFX UI
mvn exec:java -Dexec.args="--console"   # console UI
```

Without Maven: download `sqlite-jdbc`, `slf4j-api` and the OpenJFX `base`/`graphics`/`controls`
jars for your OS into `./lib`, then:

```bash
./build.sh
./run.sh                 # JavaFX UI
./run.sh --console       # console UI
```

Configuration lives in `config/coderev.properties` (database path, log file, and the
four recommendation weights). Runtime data goes to `data/`, logs to `logs/`,
exports to `exports/` and backups to `backups/` (all git-ignored).

## Syllabus mapping

| Unit | Where it shows up |
| --- | --- |
| 1 — OOP, packages, build tools | layered packages, enums with behaviour, `pom.xml` + build scripts |
| 2 — Exceptions, collections, generics | custom exception hierarchy, `GenericDao<T,ID>`, `Optional`, `EnumMap`/`TreeMap` analytics |
| 3 — File I/O, persistence, logging | `CsvIO` character streams, `BackupService` serialization, `AppLogger` (JUL), `AppConfig` properties |
| 4 — JDBC | SQLite schema, `PreparedStatement` everywhere, transaction in `RevisionService.recordRevision`, DAO pattern |
| 5 — JavaFX | `CodeRevApp` Stage/Scene, BorderPane/VBox/HBox/GridPane layouts, TableView/ListView/ComboBox/Spinner/DatePicker, event handlers, Alert feedback |

## Team GSK (3C33)

- Gaurish Narang (1024030485)
- Krishna Vig (1024031092)
- Sagar (1024030489)
