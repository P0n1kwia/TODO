package pl.mojrytm.ui;

import java.time.LocalDate;
import java.util.Optional;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pl.mojrytm.model.Note;
import pl.mojrytm.service.WeekRow;
import pl.mojrytm.service.WeekSummary;
import pl.mojrytm.service.Weeks;

/**
 * Przegląd tygodnia: jeden na tydzień (poniedziałek–niedziela), obok wynik tygodnia z warstwy service.
 * Zmiana tygodnia zapisuje niezapisany tekst, a pusty tekst usuwa przegląd.
 */
final class WeeklyReviewPanel extends VBox {

    private final AppModel model;
    private final Label range = new Label();
    private final Button previous = new Button(Messages.get("journal.weekly.previous"));
    private final Button next = new Button(Messages.get("journal.weekly.next"));
    private final Button current = new Button(Messages.get("journal.weekly.current"));
    private final TextArea text = new TextArea();
    private final Button save = new Button(Messages.get("journal.weekly.save"));
    private final Label dirtyLabel = new Label(Messages.get("journal.dirty"));
    private final VBox summary = new VBox(6);

    private LocalDate weekStart;
    private boolean loading;

    WeeklyReviewPanel(AppModel model) {
        super(10);
        this.model = model;
        this.weekStart = Weeks.startOf(model.today());
        getStyleClass().add("journal-panel");

        Label title = new Label(Messages.get("journal.weekly.title"));
        title.getStyleClass().add("section-title");
        range.setId("weekly-range");
        range.getStyleClass().add("habit-name");
        previous.setId("weekly-previous");
        next.setId("weekly-next");
        current.setId("weekly-current");
        HBox navigation = new HBox(8, previous, range, next, current);
        navigation.setAlignment(Pos.CENTER_LEFT);

        text.setId("weekly-text");
        text.setPromptText(Messages.get("journal.weekly.prompt"));
        text.setPrefRowCount(6);
        text.setWrapText(true);
        save.setId("weekly-save");
        save.getStyleClass().add("primary-button");
        dirtyLabel.setId("weekly-dirty");
        dirtyLabel.getStyleClass().add("form-problem");
        HBox actions = new HBox(12, save, dirtyLabel);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox editor = new VBox(8, text, actions);
        HBox.setHgrow(editor, Priority.ALWAYS);

        summary.setId("weekly-summary");
        summary.getStyleClass().add("summary-box");
        summary.setMinWidth(280);
        summary.setPrefWidth(320);
        HBox body = new HBox(16, editor, summary);
        getChildren().addAll(title, navigation, body);

        previous.setOnAction(event -> switchTo(weekStart.minusWeeks(1)));
        next.setOnAction(event -> switchTo(weekStart.plusWeeks(1)));
        current.setOnAction(event -> switchTo(Weeks.startOf(model.today())));
        save.setOnAction(event -> save());
        text.textProperty().addListener((observable, old, now) -> refreshControls());
        model.dataProperty().addListener((observable, old, now) -> {
            rebuildSummary();
            refreshControls();
        });
        model.todayProperty().addListener((observable, old, now) -> refreshControls());
        load();
    }

    /** Zapisuje przegląd, jeśli są niezapisane zmiany (przy zmianie tygodnia i przy opuszczeniu ekranu). */
    void saveIfDirty() {
        if (isDirty()) {
            save();
        }
    }

    private void switchTo(LocalDate newWeekStart) {
        saveIfDirty();
        weekStart = newWeekStart;
        load();
    }

    private void load() {
        loading = true;
        try {
            text.setText(NoteState.of(savedReview().orElse(null)).text());
        } finally {
            loading = false;
        }
        range.setText(Formats.weekRange(weekStart));
        rebuildSummary();
        refreshControls();
    }

    private Optional<Note> savedReview() {
        return model.noteBoard().weeklyReview(model.data(), weekStart);
    }

    private NoteState state() {
        return NoteState.ofFields(text.getText(), "", null);
    }

    private boolean isDirty() {
        return !state().equals(NoteState.of(savedReview().orElse(null)));
    }

    private void save() {
        NoteState state = state();
        boolean removing = state.isEmpty();
        LocalDate week = weekStart;
        if (model.update(data -> model.notes().saveWeeklyReview(data, model.today(), week, state.text()))) {
            model.showInfo(Messages.format(removing ? "journal.weekly.removed.info" : "journal.weekly.saved.info",
                    Formats.weekRange(week)));
        }
    }

    private void refreshControls() {
        if (loading) {
            return;
        }
        boolean dirty = isDirty();
        save.setDisable(!dirty);
        dirtyLabel.setVisible(dirty);
        next.setDisable(weekStart.plusWeeks(1).isAfter(model.today()));
    }

    private void rebuildSummary() {
        summary.getChildren().clear();
        Label heading = new Label(Messages.get("journal.weekly.summary"));
        heading.getStyleClass().add("habit-name");
        summary.getChildren().add(heading);
        WeekSummary result = model.board().weekSummary(model.data(), weekStart, model.today());
        if (result.rows().isEmpty()) {
            Label empty = new Label(Messages.get("journal.weekly.summary.empty"));
            empty.getStyleClass().add("habit-meta");
            empty.setWrapText(true);
            summary.getChildren().add(empty);
            return;
        }
        for (WeekRow row : result.rows()) {
            Label line = new Label(Messages.format("journal.weekly.row", row.habit().name(),
                    Formats.progress(row.week().progress()), Formats.streak(row.streak())));
            line.getStyleClass().add("summary-row");
            line.setWrapText(true);
            summary.getChildren().add(line);
        }
        result.best().ifPresent(row -> summary.getChildren().add(highlight("journal.weekly.best", "summary-best", row)));
        result.weakest().ifPresent(row ->
                summary.getChildren().add(highlight("journal.weekly.weakest", "summary-weakest", row)));
    }

    private static Label highlight(String key, String styleClass, WeekRow row) {
        Label label = new Label(Messages.format(key, row.habit().name(), Formats.progress(row.week().progress())));
        label.getStyleClass().addAll("summary-highlight", styleClass);
        label.setWrapText(true);
        return label;
    }
}
