package coderev.ui;

import coderev.model.Difficulty;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;

/** Add-problem form with JavaFX controls and layout management (GridPane). */
public class ProblemFormDialog extends Dialog<ProblemFormDialog.Result> {

    private final TextField titleField = new TextField();
    private final TextField platformField = new TextField("LeetCode");
    private final TextField topicField = new TextField();
    private final ComboBox<Difficulty> difficultyBox = new ComboBox<>();
    private final Spinner<Integer> confidenceSpinner = new Spinner<>(1, 5, 3);
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final TextArea notesArea = new TextArea();
    private final TextField urlField = new TextField();

    public ProblemFormDialog() {
        setTitle("Add solved problem");
        setHeaderText("Record a problem you solved");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        difficultyBox.getItems().setAll(Difficulty.values());
        difficultyBox.setValue(Difficulty.MEDIUM);
        notesArea.setPrefRowCount(3);
        notesArea.setPromptText("How did you solve it? Key idea, pattern, pitfalls...");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        int row = 0;
        grid.add(new Label("Title:"), 0, row);        grid.add(titleField, 1, row++);
        grid.add(new Label("Platform:"), 0, row);     grid.add(platformField, 1, row++);
        grid.add(new Label("Topic:"), 0, row);        grid.add(topicField, 1, row++);
        grid.add(new Label("Difficulty:"), 0, row);   grid.add(difficultyBox, 1, row++);
        grid.add(new Label("Confidence (1-5):"), 0, row); grid.add(confidenceSpinner, 1, row++);
        grid.add(new Label("Date solved:"), 0, row);  grid.add(datePicker, 1, row++);
        grid.add(new Label("Approach notes:"), 0, row); grid.add(notesArea, 1, row++);
        grid.add(new Label("URL:"), 0, row);          grid.add(urlField, 1, row);
        getDialogPane().setContent(grid);

        setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            return new Result(titleField.getText(), platformField.getText(), topicField.getText(),
                    difficultyBox.getValue(), confidenceSpinner.getValue(), datePicker.getValue(),
                    notesArea.getText(), urlField.getText());
        });
    }

    /** Values the user entered; the service layer validates them. */
    public static final class Result {
        public final String title;
        public final String platform;
        public final String topic;
        public final Difficulty difficulty;
        public final int confidence;
        public final LocalDate dateSolved;
        public final String notes;
        public final String url;

        Result(String title, String platform, String topic, Difficulty difficulty,
               int confidence, LocalDate dateSolved, String notes, String url) {
            this.title = title;
            this.platform = platform;
            this.topic = topic;
            this.difficulty = difficulty;
            this.confidence = confidence;
            this.dateSolved = dateSolved;
            this.notes = notes;
            this.url = url;
        }
    }
}
