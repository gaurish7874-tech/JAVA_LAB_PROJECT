# CodeRev architecture

## Layers

| Layer | Package | Responsibility |
| --- | --- | --- |
| UI | `coderev.ui`, `coderev.console` | JavaFX tabs/dialogs and a menu-driven console; both call only services |
| Service | `coderev.service` | Validation, scheduling, recommendation, analytics; transaction boundaries |
| DAO | `coderev.dao` | `GenericDao<T,ID>` contract and JDBC implementations; all SQL lives here |
| Model | `coderev.model` | Serializable POJOs and enums with behaviour |
| Cross-cutting | `coderev.io`, `coderev.util`, `coderev.exception` | Config, CSV, backup, logging, validation, exception hierarchy |

## Scheduling model (deterministic)

```
nextInterval = baseInterval(confidence) x difficultyMultiplier x growth(revisionCount)
```

- baseInterval: VERY_LOW 1d, LOW 2d, MEDIUM 4d, HIGH 7d, VERY_HIGH 14d
- difficultyMultiplier: HARD 0.75, MEDIUM 1.0, EASY 1.25
- growth: 1 + 0.5 x min(revisionCount, 4)
- result clamped to [1, 60] days

Scheduling answers "when should this problem be revised again?" while the
priority scorer answers "which due problem should be revised today?":

```
priority = wc x confidenceNeed + wd x difficultyWeight + wr x recencyDays + wf x revisionCount
```

Weights come from `config/coderev.properties`; the recommendation view shows the
full arithmetic so the choice is always explainable.

## Data model (SQLite)

- `problems(id, title, platform, topic, difficulty, confidence, date_solved,
  last_revised, next_revision, revision_count, approach_notes, url)` with
  `UNIQUE(title, platform)`
- `revision_history(id, problem_id FK ON DELETE CASCADE, revised_at,
  previous_confidence, new_confidence, revision_number, performance_percent)`

`RevisionService.recordRevision` writes the history row and the problem update in
one transaction, so a failure never leaves the two out of sync.
