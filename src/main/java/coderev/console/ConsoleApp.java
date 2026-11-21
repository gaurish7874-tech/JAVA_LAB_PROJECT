package coderev.console;

import coderev.dao.Database;
import coderev.dao.JdbcProblemDao;
import coderev.dao.JdbcRevisionDao;
import coderev.dao.ProblemDao;
import coderev.dao.RevisionDao;
import coderev.exception.CodeRevException;
import coderev.io.AppConfig;
import coderev.io.BackupService;
import coderev.io.CsvIO;
import coderev.model.Difficulty;
import coderev.model.Problem;
import coderev.model.RevisionRecord;
import coderev.service.AnalyticsService;
import coderev.service.PriorityScorer;
import coderev.service.ProblemService;
import coderev.service.RevisionService;
import coderev.service.RevisionScheduler;
import coderev.service.SpacedRepetitionScheduler;
import coderev.util.AppLogger;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/** Text-mode front end: the same services as the JavaFX UI, no toolkit needed.
 *  Run with: java coderev.Main --console */
public class ConsoleApp {
    private final ProblemService problemService;
    private final RevisionService revisionService;
    private final AnalyticsService analyticsService;
    private final CsvIO csvIO = new CsvIO();
    private final BackupService backupService = new BackupService();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.load();
        AppLogger.init(config);
        new ConsoleApp(config).run();
    }

    ConsoleApp(AppConfig config) throws CodeRevException {
        Database db = new Database(config.dbPath());
        ProblemDao problemDao = new JdbcProblemDao(db);
        RevisionDao revisionDao = new JdbcRevisionDao(db);
        RevisionScheduler scheduler = new SpacedRepetitionScheduler();
        PriorityScorer scorer = new PriorityScorer(
                config.getDouble("weight.confidence"), config.getDouble("weight.difficulty"),
                config.getDouble("weight.recency"), config.getDouble("weight.frequency"));
        this.problemService = new ProblemService(problemDao, scheduler);
        this.revisionService = new RevisionService(db, problemDao, revisionDao, scheduler, scorer);
        this.analyticsService = new AnalyticsService(problemDao, revisionDao);
    }

    void run() {
        System.out.println("=== CodeRev - Smart LeetCode Revision System ===");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1": addProblem(); break;
                    case "2": listProblems(); break;
                    case "3": recordRevision(); break;
                    case "4": showDueToday(); break;
                    case "5": showRecommendation(); break;
                    case "6": showAnalytics(); break;
                    case "7": exportCsv(); break;
                    case "8": backup(); break;
                    case "9": deleteProblem(); break;
                    case "0": running = false; break;
                    default: System.out.println("Unknown option: " + choice);
                }
            } catch (CodeRevException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Unexpected error: " + e.getMessage());
            }
        }
        System.out.println("Goodbye. Keep revising!");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("1. Add solved problem");
        System.out.println("2. List all problems");
        System.out.println("3. Record a revision");
        System.out.println("4. Show due today");
        System.out.println("5. Today's recommendation");
        System.out.println("6. Analytics summary");
        System.out.println("7. Export problems to CSV");
        System.out.println("8. Backup data (.ser)");
        System.out.println("9. Delete a problem");
        System.out.println("0. Exit");
        System.out.print("Choice: ");
    }

    private void addProblem() throws CodeRevException {
        System.out.print("Title: ");
        String title = scanner.nextLine();
        System.out.print("Platform (e.g. LeetCode): ");
        String platform = scanner.nextLine();
        System.out.print("Topic (e.g. Dynamic Programming): ");
        String topic = scanner.nextLine();
        System.out.print("Difficulty (EASY/MEDIUM/HARD): ");
        Difficulty difficulty = Difficulty.fromString(scanner.nextLine());
        System.out.print("Confidence (1-5): ");
        int confidence = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Date solved (yyyy-mm-dd, empty = today): ");
        String dateText = scanner.nextLine().trim();
        LocalDate dateSolved = dateText.isEmpty() ? LocalDate.now() : LocalDate.parse(dateText);
        System.out.print("Approach notes: ");
        String notes = scanner.nextLine();
        System.out.print("Problem URL (optional): ");
        String url = scanner.nextLine();

        Problem saved = problemService.addProblem(title, platform, topic, difficulty,
                confidence, dateSolved, notes, url);
        System.out.println("Saved as #" + saved.getId() + ". First revision scheduled on "
                + saved.getNextRevision());
    }

    private void listProblems() throws CodeRevException {
        List<Problem> all = problemService.listAll();
        if (all.isEmpty()) {
            System.out.println("No problems yet. Add one with option 1.");
            return;
        }
        for (Problem p : all) {
            System.out.printf("#%-3d %-35s %-12s %-6s conf=%d rev=%d next=%s%n",
                    p.getId(), p.getTitle(), p.getTopic(), p.getDifficulty(),
                    p.getConfidence().getLevel(), p.getRevisionCount(), p.getNextRevision());
        }
    }

    private void recordRevision() throws CodeRevException {
        listProblems();
        System.out.print("Problem id: ");
        long id = Long.parseLong(scanner.nextLine().trim());
        System.out.print("New confidence (1-5): ");
        int confidence = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Performance this revision (0-100%): ");
        int performance = Integer.parseInt(scanner.nextLine().trim());
        RevisionRecord record = revisionService.recordRevision(id, confidence, performance);
        System.out.println("Recorded: " + record);
        System.out.println("Next revision: " + problemService.getProblem(id).getNextRevision());
    }

    private void showDueToday() throws CodeRevException {
        List<Problem> due = revisionService.dueToday();
        if (due.isEmpty()) {
            System.out.println("Nothing due today.");
            return;
        }
        System.out.println(due.size() + " problem(s) due:");
        for (Problem p : due) {
            System.out.println("  #" + p.getId() + " " + p.getTitle()
                    + " (was due " + p.getNextRevision() + ")");
        }
    }

    private void showRecommendation() throws CodeRevException {
        Optional<RevisionService.Recommendation> rec = revisionService.recommendToday();
        if (rec.isEmpty()) {
            System.out.println("No recommendation: nothing is due today.");
            return;
        }
        RevisionService.Recommendation r = rec.get();
        System.out.println("Revise today: " + r.getProblem().getTitle()
                + " [" + r.getProblem().getTopic() + "]");
        System.out.println("Why: " + r.getExplanation());
    }

    private void showAnalytics() throws CodeRevException {
        AnalyticsService.Summary s = analyticsService.summary();
        System.out.println("Total problems: " + s.getTotalProblems());
        System.out.println("Due today:      " + s.getDueToday());
        System.out.println("Revisions done: " + s.getTotalRevisions());
        System.out.println("By difficulty:  " + s.getByDifficulty());
        System.out.println("By confidence:  " + s.getByConfidence());
        System.out.println("Topics:");
        for (AnalyticsService.TopicStats ts : s.getTopicStats()) {
            System.out.printf("  %-25s %d problem(s), avg confidence %.1f, %d due%n",
                    ts.getTopic(), ts.getCount(), ts.getAverageConfidence(), ts.getDue());
        }
        AnalyticsService.TopicStats weakest = s.weakestTopic();
        if (weakest != null) {
            System.out.println("Weakest topic: " + weakest.getTopic()
                    + " (avg confidence " + String.format("%.1f", weakest.getAverageConfidence()) + ")");
        }
    }

    private void exportCsv() throws CodeRevException {
        String target = "exports/problems-" + LocalDate.now() + ".csv";
        csvIO.exportProblems(problemService.listAll(), Paths.get(target));
        System.out.println("Exported to " + target);
    }

    private void backup() throws CodeRevException {
        String target = "backups/coderev-" + LocalDate.now() + ".ser";
        backupService.backup(problemService.listAll(), revisionService.allHistory(),
                Paths.get(target));
        System.out.println("Backup written to " + target);
    }

    private void deleteProblem() throws CodeRevException {
        listProblems();
        System.out.print("Problem id to delete: ");
        long id = Long.parseLong(scanner.nextLine().trim());
        problemService.deleteProblem(id);
        System.out.println("Deleted #" + id);
    }
}
