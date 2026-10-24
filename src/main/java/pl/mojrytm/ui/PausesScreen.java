package pl.mojrytm.ui;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pl.mojrytm.model.Pause;

/**
 * Ekran „Pauzy": dni choroby lub wyjazdu, które nie przerywają serii i nie liczą się do procentów.
 * Pauzę można dodać wstecz (uzupełnienie) i na przyszłość (plan).
 */
final class PausesScreen extends VBox {

    private final AppModel model;

    PausesScreen(AppModel model) {
        super(12);
        this.model = model;
        getStyleClass().add("screen-content");
        model.dataProperty().addListener((observable, previous, current) -> rebuild());
        rebuild();
    }

    private void rebuild() {
        getChildren().clear();
        Label title = new Label(Messages.get("pauses.title"));
        title.getStyleClass().add("screen-title");
        Label intro = new Label(Messages.get("pauses.intro"));
        intro.getStyleClass().add("habit-meta");
        intro.setWrapText(true);
        getChildren().addAll(title, intro, form());

        List<Pause> pauses = model.data().pauses().stream()
                .sorted(Comparator.comparing(Pause::from).reversed().thenComparing(Pause::to, Comparator.reverseOrder()))
                .toList();
        Label heading = new Label(Messages.get("pauses.list"));
        heading.getStyleClass().add("section-title");
        getChildren().add(heading);
        if (pauses.isEmpty()) {
            Label empty = new Label(Messages.get("pauses.empty"));
            empty.getStyleClass().add("habit-meta");
            getChildren().add(empty);
        }
        pauses.forEach(pause -> getChildren().add(row(pause)));
    }

    private Node form() {
        DatePicker from = datePicker("pause-from");
        DatePicker to = datePicker("pause-to");
        ComboBox<String> reason = new ComboBox<>();
        reason.setId("pause-reason");
        reason.setEditable(true);
        reason.getItems().addAll(Messages.get("pauses.reason.illness"), Messages.get("pauses.reason.trip"));
        reason.setPrefWidth(180);

        Button add = new Button(Messages.get("pauses.add"));
        add.setId("pause-add");
        add.getStyleClass().add("primary-button");
        Label problem = new Label();
        problem.getStyleClass().add("form-problem");

        Runnable refresh = () -> {
            Optional<PauseForm.Problem> found = currentForm(from, to, reason).problem();
            add.setDisable(found.isPresent());
            problem.setText(found.map(p -> Messages.get("pauses.form.problem." + p.name())).orElse(""));
        };
        from.valueProperty().addListener((observable, previous, current) -> refresh.run());
        to.valueProperty().addListener((observable, previous, current) -> refresh.run());
        add.setOnAction(event -> {
            Pause pause = currentForm(from, to, reason).toPause();
            if (model.update(data -> model.pauses().add(data, pause))) {
                model.showInfo(Messages.format("pauses.added.info", describe(pause)));
            }
        });
        refresh.run();

        HBox fields = new HBox(12,
                labeled(Messages.get("pauses.from"), from),
                labeled(Messages.get("pauses.to"), to),
                labeled(Messages.get("pauses.reason"), reason),
                add);
        fields.setAlignment(Pos.BOTTOM_LEFT);
        return new VBox(6, fields, problem);
    }

    private DatePicker datePicker(String id) {
        DatePicker picker = new DatePicker(model.today());
        picker.setId(id);
        picker.setConverter(new DateConverter());
        picker.setPrefWidth(140);
        return picker;
    }

    private static PauseForm currentForm(DatePicker from, DatePicker to, ComboBox<String> reason) {
        return new PauseForm(from.getValue(), to.getValue(), reason.getEditor().getText());
    }

    private static Node labeled(String label, Node field) {
        Label caption = new Label(label);
        caption.getStyleClass().add("habit-meta");
        return new VBox(4, caption, field);
    }

    private Node row(Pause pause) {
        Label range = new Label(Formats.numericRange(pause.from(), pause.to()));
        range.getStyleClass().add("habit-name");
        VBox info = new VBox(2, range);
        if (pause.reason() != null) {
            Label reason = new Label(pause.reason());
            reason.getStyleClass().add("habit-meta");
            info.getChildren().add(reason);
        }
        HBox.setHgrow(info, Priority.ALWAYS);

        Button remove = new Button(Messages.get("pauses.remove"));
        remove.getStyleClass().add("remove-button");
        remove.setOnAction(event -> {
            if (model.update(data -> model.pauses().remove(data, pause))) {
                model.showInfo(Messages.format("pauses.removed.info", describe(pause)));
            }
        });

        HBox row = new HBox(12, info, remove);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("habit-card");
        return row;
    }

    private static String describe(Pause pause) {
        return Formats.numericRange(pause.from(), pause.to());
    }
}
