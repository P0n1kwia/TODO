package pl.mojrytm.ui;

import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pl.mojrytm.model.AppData;

/**
 * Pola jednej notatki: tekst, tagi (po przecinku) i powiązany nawyk. Zmiany pól zgłasza wskazanemu
 * słuchaczowi, ale nie wtedy, gdy pola ładuje program ({@link #load}).
 */
final class NoteFields extends VBox {

    private final TextArea text = new TextArea();
    private final TextField tags = new TextField();
    private final ComboBox<HabitChoice> habit = new ComboBox<>();
    private boolean loading;
    private Runnable onChange = () -> { };

    /**
     * @param idPrefix  przedrostek identyfikatorów pól (np. "daily" daje "daily-text", "daily-tags", "daily-habit")
     * @param promptKey klucz podpowiedzi w polu tekstu
     * @param rows      wysokość pola tekstu w wierszach
     */
    NoteFields(String idPrefix, String promptKey, int rows) {
        super(8);
        text.setId(idPrefix + "-text");
        text.setPromptText(Messages.get(promptKey));
        text.setPrefRowCount(rows);
        text.setWrapText(true);
        tags.setId(idPrefix + "-tags");
        tags.setPromptText(Messages.get("journal.fields.tags.prompt"));
        tags.setPrefColumnCount(24);
        habit.setId(idPrefix + "-habit");
        habit.setPrefWidth(240);

        HBox details = new HBox(12, labeled(Messages.get("journal.fields.tags"), tags),
                labeled(Messages.get("journal.fields.habit"), habit));
        getChildren().addAll(text, details);

        text.textProperty().addListener((observable, previous, current) -> changed());
        tags.textProperty().addListener((observable, previous, current) -> changed());
        habit.valueProperty().addListener((observable, previous, current) -> changed());
    }

    void setOnChange(Runnable listener) {
        this.onChange = listener;
    }

    /** Stan pól jako porównywalna treść notatki. */
    NoteState state() {
        HabitChoice choice = habit.getValue();
        return NoteState.ofFields(text.getText(), tags.getText(), choice == null ? null : choice.habitId());
    }

    /** Wypełnia pola zapisaną treścią (bez zgłaszania zmiany); lista nawyków wynika z {@code data}. */
    void load(NoteState state, AppData data) {
        loading = true;
        try {
            text.setText(state.text());
            tags.setText(String.join(", ", state.tags()));
            setHabits(data, state.habitId());
        } finally {
            loading = false;
        }
    }

    /** Odświeża listę nawyków (np. po zarchiwizowaniu), zachowując wybór. */
    void refreshHabits(AppData data) {
        loading = true;
        try {
            HabitChoice choice = habit.getValue();
            setHabits(data, choice == null ? null : choice.habitId());
        } finally {
            loading = false;
        }
    }

    /** Czyści pola (np. po dodaniu notatki); zgłasza zmianę raz. */
    void clear(AppData data) {
        load(NoteState.EMPTY, data);
        changed();
    }

    private void setHabits(AppData data, String selectedHabitId) {
        List<HabitChoice> choices = HabitChoice.list(data, selectedHabitId);
        habit.getItems().setAll(choices);
        habit.getSelectionModel().select(HabitChoice.find(choices, selectedHabitId));
    }

    private void changed() {
        if (!loading) {
            onChange.run();
        }
    }

    private static Node labeled(String label, Node field) {
        Label caption = new Label(label);
        caption.getStyleClass().add("habit-meta");
        return new VBox(4, caption, field);
    }
}
