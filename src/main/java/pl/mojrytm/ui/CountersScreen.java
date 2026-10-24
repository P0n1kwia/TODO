package pl.mojrytm.ui;

import java.util.Optional;
import java.util.function.Predicate;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pl.mojrytm.model.Counter;
import pl.mojrytm.model.Ids;
import pl.mojrytm.service.CounterRow;

/**
 * Ekran „Liczniki": ile pełnych dni minęło od startu („dni od" lub „dni bez"). Zerowanie i usuwanie
 * nie da się cofnąć, więc pytają o potwierdzenie.
 */
final class CountersScreen extends VBox {

    private final AppModel model;
    private final Predicate<String> confirm;

    CountersScreen(AppModel model) {
        this(model, Confirmations::ask);
    }

    /** @param confirm pokazuje pytanie (tekst) i zwraca true, gdy użytkownik się zgodził */
    CountersScreen(AppModel model, Predicate<String> confirm) {
        super(12);
        this.model = model;
        this.confirm = confirm;
        getStyleClass().add("screen-content");
        model.dataProperty().addListener((observable, previous, current) -> rebuild());
        model.todayProperty().addListener((observable, previous, current) -> rebuild());
        rebuild();
    }

    private void rebuild() {
        getChildren().clear();
        Label title = new Label(Messages.get("counters.title"));
        title.getStyleClass().add("screen-title");
        Label intro = new Label(Messages.get("counters.intro"));
        intro.getStyleClass().add("habit-meta");
        intro.setWrapText(true);
        getChildren().addAll(title, intro, form());

        Label heading = new Label(Messages.get("counters.list"));
        heading.getStyleClass().add("section-title");
        getChildren().add(heading);
        var rows = model.counterBoard().rows(model.data(), model.today());
        if (rows.isEmpty()) {
            Label empty = new Label(Messages.get("counters.empty"));
            empty.getStyleClass().add("habit-meta");
            getChildren().add(empty);
        }
        rows.forEach(row -> getChildren().add(card(row)));
    }

    private Node form() {
        TextField name = new TextField();
        name.setId("counter-name");
        name.setPromptText(Messages.get("counters.form.name.prompt"));
        name.setPrefColumnCount(24);
        DatePicker start = new DatePicker(model.today());
        start.setId("counter-start");
        start.setConverter(new DateConverter());
        start.setPrefWidth(140);

        Button add = new Button(Messages.get("counters.add"));
        add.setId("counter-add");
        add.getStyleClass().add("primary-button");
        Label problem = new Label();
        problem.getStyleClass().add("form-problem");

        Runnable refresh = () -> {
            Optional<CounterForm.Problem> found = currentForm(name, start).problem();
            add.setDisable(found.isPresent());
            problem.setText(found.map(p -> Messages.get("counters.form.problem." + p.name())).orElse(""));
        };
        name.textProperty().addListener((observable, previous, current) -> refresh.run());
        start.valueProperty().addListener((observable, previous, current) -> refresh.run());
        add.setOnAction(event -> {
            Counter counter = currentForm(name, start).toCounter(Ids.newId());
            if (model.update(data -> model.counters().add(data, counter))) {
                model.showInfo(Messages.format("counters.added.info", counter.name()));
            }
        });
        refresh.run();

        HBox fields = new HBox(12,
                labeled(Messages.get("counters.form.name"), name),
                labeled(Messages.get("counters.form.start"), start),
                add);
        fields.setAlignment(Pos.BOTTOM_LEFT);
        return new VBox(6, fields, problem);
    }

    private CounterForm currentForm(TextField name, DatePicker start) {
        return new CounterForm(name.getText(), start.getValue(), model.today());
    }

    private static Node labeled(String label, Node field) {
        Label caption = new Label(label);
        caption.getStyleClass().add("habit-meta");
        return new VBox(4, caption, field);
    }

    private Node card(CounterRow row) {
        Counter counter = row.counter();
        Label days = new Label(String.valueOf(row.days()));
        days.getStyleClass().add("counter-days");
        Label unit = new Label(Formats.dayUnit(row.days()));
        unit.getStyleClass().add("habit-meta");
        VBox number = new VBox(0, days, unit);
        number.setAlignment(Pos.CENTER);
        number.setMinWidth(72);

        Label name = new Label(counter.name());
        name.getStyleClass().add("habit-name");
        Label since = new Label(Messages.format("counters.since", Formats.numericDate(counter.startDate())));
        since.getStyleClass().addAll("habit-meta", "counter-since");
        VBox info = new VBox(2, name, since);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button restart = new Button(Messages.get("counters.restart"));
        restart.getStyleClass().add("restart-button");
        restart.setDisable(counter.startDate().equals(model.today()));
        restart.setOnAction(event -> restart(row));
        Button rename = new Button(Messages.get("counters.rename"));
        rename.getStyleClass().addAll("edit-button", "rename-counter-button");
        rename.setOnAction(event -> rename(counter));
        Button remove = new Button(Messages.get("counters.remove"));
        remove.getStyleClass().add("remove-button");
        remove.setOnAction(event -> remove(counter));

        HBox card = new HBox(16, number, info, restart, rename, remove);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().addAll("habit-card", "counter-card");
        return card;
    }

    private void restart(CounterRow row) {
        Counter counter = row.counter();
        if (confirm.test(Messages.format("counters.restart.confirm", counter.name(), Formats.days(row.days())))
                && model.update(data -> model.counters().restart(data, counter.id(), model.today()))) {
            model.showInfo(Messages.format("counters.restarted.info", counter.name()));
        }
    }

    private void rename(Counter counter) {
        NameDialog.show(Windows.of(this), Messages.get("dialog.name.counter.title"),
                        Messages.get("dialog.name.counter.prompt"), counter.name(), name -> false)
                .filter(name -> !name.equals(counter.name()))
                .ifPresent(name -> model.update(data -> model.counters().rename(data, counter.id(), name)));
    }

    private void remove(Counter counter) {
        if (confirm.test(Messages.format("counters.remove.confirm", counter.name()))
                && model.update(data -> model.counters().remove(data, counter.id()))) {
            model.showInfo(Messages.format("counters.removed.info", counter.name()));
        }
    }
}
