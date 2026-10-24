package pl.mojrytm.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Wolne notatki: dowolnie wiele, z dzisiejszą datą, tagami i opcjonalnym nawykiem. */
final class FreeNotesPanel extends VBox {

    private final AppModel model;
    private final NoteFields fields = new NoteFields("free", "journal.free.prompt", 3);
    private final Button add = new Button(Messages.get("journal.free.add"));

    FreeNotesPanel(AppModel model) {
        super(10);
        this.model = model;
        getStyleClass().add("journal-panel");

        Label title = new Label(Messages.get("journal.free.title"));
        title.getStyleClass().add("section-title");
        add.setId("free-add");
        add.getStyleClass().add("primary-button");
        getChildren().addAll(title, fields, add);

        add.setOnAction(event -> add());
        fields.setOnChange(this::refreshControls);
        model.dataProperty().addListener((observable, old, current) -> fields.refreshHabits(current));
        fields.load(NoteState.EMPTY, model.data());
        refreshControls();
    }

    private void add() {
        NoteState state = fields.state();
        if (model.update(data -> model.notes().addFree(data, model.today(), state.text(), state.tags(),
                state.habitId()))) {
            fields.clear(model.data());
            model.showInfo(Messages.get("journal.free.added.info"));
        }
    }

    private void refreshControls() {
        add.setDisable(fields.state().isEmpty());
    }
}
