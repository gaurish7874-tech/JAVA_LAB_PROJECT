package coderev.ui;

import coderev.dao.Database;
import coderev.dao.JdbcProblemDao;
import coderev.dao.JdbcRevisionDao;
import coderev.dao.ProblemDao;
import coderev.dao.RevisionDao;
import coderev.exception.CodeRevException;
import coderev.io.AppConfig;
import coderev.io.BackupService;
import coderev.io.CsvIO;
import coderev.model.Problem;
import coderev.model.RevisionRecord;
import coderev.service.AnalyticsService;
import coderev.service.PriorityScorer;
import coderev.service.ProblemService;
import coderev.service.RevisionService;
import coderev.service.RevisionScheduler;
import coderev.service.SpacedRepetitionScheduler;
import coderev.util.AppLogger;

import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/** JavaFX front end: dashboard, problem table and analytics tabs. */
public class CodeRevApp extends Application {
    private static final Logger LOG = AppLogger.getLogger(CodeRevApp.class);

    private ProblemService problemService;
    private RevisionService revisionService;
    private AnalyticsService analyticsService;
    private final CsvIO csvIO = new CsvIO();
    private final BackupService backupService = new BackupService();

    private TableView<Problem> problemTable;
    private ListView<String> dueList;
    private Label recommendationLabel;
    private Label analyticsLabel;

    @Override
    public void start(Stage stage) {
        try {
            AppConfig config = AppConfig.load();
            AppLogger.init(config);
            buildServices(config);
        } catch (CodeRevException e) {
            showError("Startup failed", e.getMessage());
            return;
        }

        stage.setTitle("CodeRev - Smart LeetCode Revision System");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(buildDashboardTab(), buildProblemsTab(), buildAnalyticsTab());

        BorderPane root = new BorderPane(tabs);
        Label header = new Label("CodeRev - revise smarter, not harder");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        BorderPane.setMargin(header, new Insets(10));
        root.setTop(header);

        stage.setScene(new Scene(root, 900, 600));
        stage.show();
        // Optional initial tab (used by demos/screenshots): -Dcoderev.tab=0|1|2
        int initialTab = Integer.getInteger("coderev.tab", 0);
        if (initialTab >= 0 && initialTab < tabs.getTabs().size()) {
            tabs.getSelectionModel().select(initialTab);
        }
        refreshAll();
        LOG.info("JavaFX UI started");
    }

    private void buildServices(AppConfig config) throws CodeRevException {
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

    private Tab buildDashboardTab() {
        dueList = new ListView<>();
        recommendationLabel = new Label();
        recommendationLabel.setWrapText(true);
        recommendationLabel.setMinHeight(48);
        recommendationLabel.setMaxWidth(Double.MAX_VALUE);
        recommendationLabel.setStyle("-fx-font-size: 13px;");

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshAll());

        VBox box = new VBox(10,
                new Label("Due today (highest priority first):"), dueList,
                new Label("Today's recommendation:"), recommendationLabel,
                refreshButton);
        box.setPadding(new Insets(15));
        VBox.setVgrow(dueList, Priority.ALWAYS);

        Tab tab = new Tab("Dashboard", box);
        return tab;
    }

    private Tab buildProblemsTab() {
        problemTable = new TableView<>();

        TableColumn<Problem, Number> idCol = new TableColumn<>("#");
        idCol.setPrefWidth(45);
        idCol.setCellValueFactory(c -> new SimpleIntegerProperty((int) c.getValue().getId()));
        TableColumn<Problem, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        titleCol.setPrefWidth(260);
        withTooltip(titleCol);
        TableColumn<Problem, String> topicCol = new TableColumn<>("Topic");
        topicCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTopic()));
        topicCol.setPrefWidth(170);
        withTooltip(topicCol);
        TableColumn<Problem, String> diffCol = new TableColumn<>("Difficulty");
        diffCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDifficulty().name()));
        diffCol.setPrefWidth(95);
        TableColumn<Problem, Number> confCol = new TableColumn<>("Confidence");
        confCol.setCellValueFactory(c ->
                new SimpleIntegerProperty(c.getValue().getConfidence().getLevel()));
        confCol.setPrefWidth(95);
        TableColumn<Problem, Number> revCol = new TableColumn<>("Revisions");
        revCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getRevisionCount()));
        revCol.setPrefWidth(85);
        TableColumn<Problem, String> nextCol = new TableColumn<>("Next revision");
        nextCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNextRevision() == null ? "-" : c.getValue().getNextRevision().toString()));
        nextCol.setPrefWidth(115);
        problemTable.getColumns().setAll(idCol, titleCol, topicCol, diffCol, confCol, revCol, nextCol);

        Button addButton = new Button("Add problem");
        addButton.setOnAction(e -> onAddProblem());
        Button reviseButton = new Button("Record revision");
        reviseButton.setOnAction(e -> onRecordRevision());
        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(e -> onDelete());
        Button exportButton = new Button("Export CSV");
        exportButton.setOnAction(e -> onExportCsv());
        Button backupButton = new Button("Backup");
        backupButton.setOnAction(e -> onBackup());
        HBox toolbar = new HBox(8, addButton, reviseButton, deleteButton, exportButton, backupButton);
        toolbar.setPadding(new Insets(10));

        BorderPane pane = new BorderPane(problemTable);
        pane.setTop(toolbar);
        return new Tab("Problems", pane);
    }

    /** Shows the full cell text in a tooltip, so truncated values stay readable. */
    private static void withTooltip(TableColumn<Problem, String> column) {
        column.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setTooltip(empty || item == null ? null : new javafx.scene.control.Tooltip(item));
            }
        });
    }

    private Tab buildAnalyticsTab() {
        analyticsLabel = new Label();
        analyticsLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        VBox box = new VBox(10, analyticsLabel);
        box.setPadding(new Insets(15));
        return new Tab("Analytics", box);
    }

    private void onAddProblem() {
        ProblemFormDialog dialog = new ProblemFormDialog();
        Optional<ProblemFormDialog.Result> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }
        ProblemFormDialog.Result r = result.get();
        try {
            Problem saved = problemService.addProblem(r.title, r.platform, r.topic, r.difficulty,
                    r.confidence, r.dateSolved, r.notes, r.url);
            showInfo("Problem saved", "'" + saved.getTitle() + "' saved. First revision on "
                    + saved.getNextRevision() + ".");
            refreshAll();
        } catch (CodeRevException e) {
            showError("Could not save problem", e.getMessage());
        }
    }

    private void onRecordRevision() {
        Problem selected = problemTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No selection", "Select a problem in the table first.");
            return;
        }
        TextInputDialog confDialog = new TextInputDialog(
                String.valueOf(selected.getConfidence().getLevel()));
        confDialog.setTitle("Record revision");
        confDialog.setHeaderText("Revising: " + selected.getTitle());
        confDialog.setContentText("New confidence (1-5):");
        Optional<String> confText = confDialog.showAndWait();
        if (confText.isEmpty()) {
            return;
        }
        TextInputDialog perfDialog = new TextInputDialog("80");
        perfDialog.setTitle("Record revision");
        perfDialog.setHeaderText("How well did it go?");
        perfDialog.setContentText("Performance (0-100%):");
        Optional<String> perfText = perfDialog.showAndWait();
        if (perfText.isEmpty()) {
            return;
        }
        try {
            int confidence = Integer.parseInt(confText.get().trim());
            int performance = Integer.parseInt(perfText.get().trim());
            RevisionRecord record = revisionService.recordRevision(
                    selected.getId(), confidence, performance);
            Problem updated = problemService.getProblem(selected.getId());
            showInfo("Revision recorded", record + "\nNext revision: " + updated.getNextRevision());
            refreshAll();
        } catch (NumberFormatException e) {
            showError("Invalid input", "Confidence and performance must be numbers.");
        } catch (CodeRevException e) {
            showError("Could not record revision", e.getMessage());
        }
    }

    private void onDelete() {
        Problem selected = problemTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No selection", "Select a problem in the table first.");
            return;
        }
        try {
            problemService.deleteProblem(selected.getId());
            refreshAll();
        } catch (CodeRevException e) {
            showError("Could not delete", e.getMessage());
        }
    }

    private void onExportCsv() {
        try {
            String target = "exports/problems-" + LocalDate.now() + ".csv";
            csvIO.exportProblems(problemService.listAll(), Paths.get(target));
            showInfo("Export complete", "Problems exported to " + target);
        } catch (CodeRevException e) {
            showError("Export failed", e.getMessage());
        }
    }

    private void onBackup() {
        try {
            String target = "backups/coderev-" + LocalDate.now() + ".ser";
            backupService.backup(problemService.listAll(), revisionService.allHistory(),
                    Paths.get(target));
            showInfo("Backup complete", "Backup written to " + target);
        } catch (CodeRevException e) {
            showError("Backup failed", e.getMessage());
        }
    }

    private void refreshAll() {
        try {
            problemTable.setItems(FXCollections.observableArrayList(problemService.listAll()));

            List<Problem> due = revisionService.dueToday();
            dueList.getItems().clear();
            for (Problem p : due) {
                dueList.getItems().add(p.getTitle() + "  [" + p.getTopic() + ", due "
                        + p.getNextRevision() + "]");
            }

            Optional<RevisionService.Recommendation> rec = revisionService.recommendToday();
            recommendationLabel.setText(rec
                    .map(r -> r.getProblem().getTitle() + "\nWhy: " + r.getExplanation())
                    .orElse("Nothing due today. Add problems or enjoy the break."));

            AnalyticsService.Summary s = analyticsService.summary();
            StringBuilder sb = new StringBuilder();
            sb.append("Total problems: ").append(s.getTotalProblems()).append('\n');
            sb.append("Due today:      ").append(s.getDueToday()).append('\n');
            sb.append("Revisions done: ").append(s.getTotalRevisions()).append('\n');
            sb.append("By difficulty:  ").append(s.getByDifficulty()).append('\n');
            sb.append("By confidence:  ").append(s.getByConfidence()).append('\n');
            sb.append("\nTopics:\n");
            for (AnalyticsService.TopicStats ts : s.getTopicStats()) {
                sb.append(String.format("  %-25s %d problem(s), avg confidence %.1f, %d due%n",
                        ts.getTopic(), ts.getCount(), ts.getAverageConfidence(), ts.getDue()));
            }
            AnalyticsService.TopicStats weakest = s.weakestTopic();
            if (weakest != null) {
                sb.append("\nWeakest topic: ").append(weakest.getTopic());
            }
            analyticsLabel.setText(sb.toString());
        } catch (CodeRevException e) {
            showError("Refresh failed", e.getMessage());
        }
    }

    private static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
