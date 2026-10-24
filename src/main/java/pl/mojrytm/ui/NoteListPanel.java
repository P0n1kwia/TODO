package pl.mojrytm.ui;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.Note;
import pl.mojrytm.model.NoteType;

/** Lista ostatnich notatek (najnowsze pierwsze) z filtrem tagu. Wolną notatkę można usunąć po potwierdzeniu. */
final class NoteListPanel extends VBox {

    /** Tyle najnowszych wpisów pokazujemy; starsze są dostępne przez wybór dnia lub tygodnia. */
    static final int LIMIT = 50;

    private final AppModel model;
    private final Predicate<String> confirm;
    private final ComboBox<String> tagFilter = new ComboBox<>();
    private final VBox list = new VBox(8);
    private boolean refreshingFilter;

    NoteListPanel(AppModel model, Predicate<String> confirm) {
        super(10);
        this.model = model;
        this.confirm = confirm;
        getStyleClass().add("journal-panel");

        Label title = new Label(Messages.get("journal.list.title"));
        title.getStyleClass().add("section-title");
        Label caption = new Label(Messages.get("journal.list.filter"));
        caption.getStyleClass().add("habit-meta");
        tagFilter.setId("note-tag-filter");
        tagFilter.setPrefWidth(200);
        HBox filter = new HBox(8, caption, tagFilter);
        filter.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(title, filter, list);

        tagFilter.valueProperty().addListener((observable, old, current) -> {
            if (!refreshingFilter) {
                rebuildList();
            }
        });
        model.dataProperty().addListener((observable, old, current) -> refresh());
        refresh();
    }

    private void refresh() {
        refreshingFilter = true;
        try {
            String selected = selectedTag();
            List<String> tags = model.noteBoard().tags(model.data());
            tagFilter.getItems().clear();
            tagFilter.getItems().add(Messages.get("journal.list.filter.all"));
            tagFilter.getItems().addAll(tags);
            boolean stillThere = selected != null && tags.contains(selected);
            tagFilter.getSelectionModel().select(stillThere ? selected : tagFilter.getItems().get(0));
        } finally {
            refreshingFilter = false;
        }
        rebuildList();
    }

    /** Wybrany tag albo null dla "Wszystkie tagi" (pierwsza pozycja listy). */
    private String selectedTag() {
        int index = tagFilter.getSelectionModel().getSelectedIndex();
        return index <= 0 ? null : tagFilter.getItems().get(index);
    }

    private void rebuildList() {
        list.getChildren().clear();
        String tag = selectedTag();
        List<Note> notes = model.noteBoard().recent(model.data(), tag, LIMIT);
        if (notes.isEmpty()) {
            Label empty = new Label(Messages.get(tag == null ? "journal.list.empty" : "journal.list.emptyFiltered"));
            empty.getStyleClass().addAll("habit-meta", "note-empty");
            list.getChildren().add(empty);
            return;
        }
        notes.forEach(note -> list.getChildren().add(card(note)));
    }

    private Node card(Note note) {
        Label type = new Label(Messages.get("journal.list.type." + note.type().name()));
        type.getStyleClass().add("note-type");
        String when = note.type() == NoteType.WEEKLY_REVIEW
                ? Messages.format("journal.list.week", Formats.weekRange(note.date()))
                : Formats.numericDate(note.date());
        Label date = new Label(when);
        date.getStyleClass().addAll("habit-meta", "note-date");
        HBox header = new HBox(8, type, date);
        header.setAlignment(Pos.CENTER_LEFT);

        Label text = new Label(note.text());
        text.getStyleClass().add("note-text");
        text.setWrapText(true);
        VBox info = new VBox(4, header, text);
        meta(note).ifPresent(meta -> info.getChildren().add(meta));
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox card = new HBox(12, info);
        card.getStyleClass().addAll("habit-card", "note-card");
        if (note.type() == NoteType.FREE) {
            Button remove = new Button(Messages.get("journal.list.remove"));
            remove.getStyleClass().add("remove-button");
            remove.setOnAction(event -> remove(note));
            card.getChildren().add(remove);
        }
        return card;
    }

    private Optional<Label> meta(Note note) {
        StringBuilder parts = new StringBuilder();
        for (String tag : note.tags()) {
            parts.append(parts.isEmpty() ? "" : " ").append('#').append(tag);
        }
        Optional<String> habit = model.data().habits().stream()
                .filter(h -> h.id().equals(note.habitId())).map(Habit::name).findFirst();
        if (habit.isPresent()) {
            parts.append(parts.isEmpty() ? "" : " · ").append(Messages.format("journal.list.habit", habit.get()));
        }
        if (parts.isEmpty()) {
            return Optional.empty();
        }
        Label label = new Label(parts.toString());
        label.getStyleClass().addAll("habit-meta", "note-meta");
        label.setWrapText(true);
        return Optional.of(label);
    }

    private void remove(Note note) {
        if (confirm.test(Messages.get("journal.list.remove.confirm"))
                && model.update(data -> model.notes().remove(data, note.id()))) {
            model.showInfo(Messages.get("journal.list.removed.info"));
        }
    }
}
