package pl.mojrytm.ui;

import java.time.LocalDate;
import java.util.Optional;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pl.mojrytm.model.Note;

/**
 * Wpis dzienny: jeden na dzień, z możliwością cofnięcia się do wcześniejszych dni (nie wybiegamy w przyszłość).
 * Zmiana dnia zapisuje niezapisany tekst, a pusty tekst usuwa wpis.
 */
final class DailyNotePanel extends VBox {

    private final AppModel model;
    private final DatePicker date = new DatePicker();
    private final Button previous = new Button(Messages.get("journal.daily.previous"));
    private final Button next = new Button(Messages.get("journal.daily.next"));
    private final Button today = new Button(Messages.get("journal.daily.today"));
    private final NoteFields fields = new NoteFields("daily", "journal.daily.prompt", 6);
    private final Label problem = new Label();
    private final Label dirtyLabel = new Label(Messages.get("journal.dirty"));
    private final Button save = new Button(Messages.get("journal.daily.save"));

    /** Dzień, do którego należą pola; zmienia się dopiero po poprawnym wyborze dnia. */
    private LocalDate shown;
    private boolean switching;

    DailyNotePanel(AppModel model) {
        super(10);
        this.model = model;
        this.shown = model.today();
        getStyleClass().add("journal-panel");

        Label title = new Label(Messages.get("journal.daily.title"));
        title.getStyleClass().add("section-title");

        date.setId("daily-date");
        date.setConverter(new DateConverter());
        date.setPrefWidth(140);
        date.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(!empty && item != null && item.isAfter(model.today()));
            }
        });
        previous.setId("daily-previous");
        next.setId("daily-next");
        today.setId("daily-today");
        problem.setId("daily-problem");
        problem.getStyleClass().add("form-problem");
        dirtyLabel.setId("daily-dirty");
        dirtyLabel.getStyleClass().add("form-problem");
        save.setId("daily-save");
        save.getStyleClass().add("primary-button");

        HBox navigation = new HBox(8, previous, date, next, today);
        navigation.setAlignment(Pos.CENTER_LEFT);
        HBox actions = new HBox(12, save, dirtyLabel);
        actions.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(title, navigation, problem, fields, actions);

        previous.setOnAction(event -> date.setValue(shown.minusDays(1)));
        next.setOnAction(event -> date.setValue(shown.plusDays(1)));
        today.setOnAction(event -> date.setValue(model.today()));
        save.setOnAction(event -> save());
        date.valueProperty().addListener((observable, old, current) -> dateChosen(current));
        fields.setOnChange(this::refreshControls);
        model.dataProperty().addListener((observable, old, current) -> {
            fields.refreshHabits(current);
            refreshControls();
        });
        model.todayProperty().addListener((observable, old, current) -> refreshControls());

        switching = true;
        date.setValue(shown);
        switching = false;
        load();
    }

    /** Zapisuje wpis, jeśli są niezapisane zmiany (przy zmianie dnia i przy opuszczeniu ekranu). */
    void saveIfDirty() {
        if (isDirty()) {
            save();
        }
    }

    private void dateChosen(LocalDate chosen) {
        if (switching) {
            return;
        }
        Optional<DailyNoteForm.Problem> found = new DailyNoteForm(chosen, model.today()).problem();
        if (found.isPresent()) {
            // pola zostają przy dotychczasowym dniu, dopóki nie wybrano poprawnego
            refreshControls();
            return;
        }
        if (chosen.equals(shown)) {
            refreshControls();
            return;
        }
        saveIfDirty();
        shown = chosen;
        load();
    }

    private void load() {
        fields.load(NoteState.of(savedNote().orElse(null)), model.data());
        refreshControls();
    }

    private Optional<Note> savedNote() {
        return model.noteBoard().dailyNote(model.data(), shown);
    }

    private boolean isDirty() {
        return !fields.state().equals(NoteState.of(savedNote().orElse(null)));
    }

    private void save() {
        NoteState state = fields.state();
        boolean removing = state.isEmpty();
        LocalDate day = shown;
        if (model.update(data -> model.notes().saveDaily(data, model.today(), day, state.text(), state.tags(),
                state.habitId()))) {
            model.showInfo(Messages.format(removing ? "journal.daily.removed.info" : "journal.daily.saved.info",
                    Formats.numericDate(day)));
        }
    }

    private void refreshControls() {
        Optional<DailyNoteForm.Problem> found = new DailyNoteForm(date.getValue(), model.today()).problem();
        problem.setText(found.map(p -> Messages.get("journal.daily.problem." + p.name())).orElse(""));
        problem.setVisible(found.isPresent());
        problem.setManaged(found.isPresent());
        boolean dirty = isDirty();
        save.setDisable(found.isPresent() || !dirty);
        dirtyLabel.setVisible(dirty);
        next.setDisable(!shown.isBefore(model.today()));
    }
}
