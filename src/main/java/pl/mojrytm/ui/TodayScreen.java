package pl.mojrytm.ui;

import java.util.List;
import java.util.OptionalDouble;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.service.DayCell;
import pl.mojrytm.service.DayStatus;
import pl.mojrytm.service.HabitRow;

/**
 * Ekran „Dziś": każdy aktywny nawyk jako karta z szybkim zaznaczeniem, serią i wykonaniem tygodnia.
 * Wszystkie liczby pochodzą z warstwy service, tu są tylko rysowane.
 */
final class TodayScreen extends VBox {

    private final AppModel model;

    TodayScreen(AppModel model) {
        super(12);
        this.model = model;
        getStyleClass().add("screen-content");
        model.dataProperty().addListener((observable, previous, current) -> rebuild());
        model.todayProperty().addListener((observable, previous, current) -> rebuild());
        rebuild();
    }

    private void rebuild() {
        getChildren().clear();
        Label title = new Label(Messages.get("today.title"));
        title.getStyleClass().add("screen-title");
        Label date = new Label(Formats.longDate(model.today()));
        date.getStyleClass().add("screen-subtitle");
        getChildren().addAll(title, date);

        if (model.board().isPaused(model.data(), model.today())) {
            Label banner = new Label(Messages.get("today.paused"));
            banner.getStyleClass().add("banner");
            banner.setWrapText(true);
            getChildren().add(banner);
        }

        List<HabitRow> rows = model.board().todayRows(model.data(), model.today());
        if (rows.isEmpty()) {
            getChildren().add(emptyState());
            return;
        }
        rows.forEach(row -> getChildren().add(card(row)));
        getChildren().add(addButton());
    }

    private Node emptyState() {
        Label heading = new Label(Messages.get("today.empty.title"));
        heading.getStyleClass().add("empty-title");
        Label hint = new Label(Messages.get("today.empty.hint"));
        hint.getStyleClass().add("habit-meta");
        hint.setWrapText(true);
        VBox box = new VBox(8, heading, hint, addButton());
        box.getStyleClass().add("empty-state");
        return box;
    }

    private Button addButton() {
        Button add = new Button(Messages.get("today.add"));
        add.getStyleClass().add("primary-button");
        add.setOnAction(event -> HabitActions.create(this, model));
        return add;
    }

    private Node card(HabitRow row) {
        Habit habit = row.habit();
        DayCell cell = row.today();

        Label name = new Label(habit.name());
        name.getStyleClass().add("habit-name");
        VBox info = new VBox(2, name, meta(Messages.format("today.week", Formats.progress(row.week()))));
        if (habit.minimumVersion() != null) {
            info.getChildren().add(meta(Messages.format("habit.minimum", habit.minimumVersion())));
        }
        info.setMinWidth(200);
        info.setPrefWidth(320);
        HBox.setHgrow(info, Priority.ALWAYS);

        Node controls = habit.kind() == HabitKind.CHECK ? checkControls(habit, cell) : countControls(habit, cell);
        Label streak = new Label(Formats.streak(row.streak()));
        streak.getStyleClass().add("streak-label");
        streak.setMinWidth(110);

        HBox card = new HBox(16, info, controls, streak);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("habit-card");
        if (cell.status() == DayStatus.DONE) {
            card.getStyleClass().add("done");
        } else if (cell.status() == DayStatus.DONE_MINIMAL) {
            card.getStyleClass().add("minimal");
        }
        if (cell.paused()) {
            card.getStyleClass().add("paused");
        }
        return card;
    }

    private static Label meta(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("habit-meta");
        label.setWrapText(true);
        return label;
    }

    private HBox checkControls(Habit habit, DayCell cell) {
        boolean fullyDone = cell.status() == DayStatus.DONE;
        Button done = actionButton(Messages.get("today.done"), fullyDone, "done-button");
        done.setOnAction(event -> setChecked(habit, !fullyDone, false));
        HBox box = new HBox(8, done);
        box.setAlignment(Pos.CENTER_RIGHT);
        if (habit.minimumVersion() != null) {
            boolean minimalDone = cell.status() == DayStatus.DONE_MINIMAL;
            Button minimum = actionButton(Messages.get("today.minimumButton"), minimalDone, "minimum-button");
            minimum.setOnAction(event -> setChecked(habit, !minimalDone, true));
            box.getChildren().add(minimum);
        }
        return box;
    }

    private HBox countControls(Habit habit, DayCell cell) {
        double amount = cell.amount();
        boolean minimalDone = cell.status() == DayStatus.DONE_MINIMAL;

        Button minus = new Button(Messages.get("count.minus"));
        minus.getStyleClass().addAll("step-button", "minus-button");
        minus.setOnAction(event -> addAmount(habit, -1));
        Button plus = new Button(Messages.get("count.plus"));
        plus.getStyleClass().addAll("step-button", "plus-button");
        plus.setOnAction(event -> addAmount(habit, 1));

        TextField field = new TextField(Formats.amount(amount));
        field.getStyleClass().add("amount-field");
        field.setPrefColumnCount(5);
        field.setAlignment(Pos.CENTER_RIGHT);
        bindAmountCommit(field, habit, minimalDone);
        Label target = new Label(Messages.format("today.of", Formats.target(habit)));
        target.getStyleClass().add("habit-meta");

        HBox box = new HBox(6, minus, field, plus, target);
        box.setAlignment(Pos.CENTER_RIGHT);
        if (habit.minimumVersion() != null) {
            Button minimum = actionButton(Messages.get("today.minimumButton"), minimalDone, "minimum-button");
            minimum.setOnAction(event -> model.update(
                    data -> model.entries().setAmount(data, habit.id(), model.today(), amount, !minimalDone)));
            box.getChildren().add(minimum);
        }
        return box;
    }

    /** Wpisaną ilość zatwierdza Enter albo wyjście z pola; niepoprawny tekst pokazuje błąd i wraca do starej ilości. */
    private void bindAmountCommit(TextField field, Habit habit, boolean minimalDone) {
        String initial = field.getText();
        boolean[] handled = {false};
        Runnable commit = () -> {
            if (handled[0] || field.getText().equals(initial)) {
                return;
            }
            OptionalDouble value = Formats.parseAmount(field.getText());
            if (value.isEmpty()) {
                model.showError(Messages.get("error.amount.invalid"));
                field.setText(initial);
                return;
            }
            handled[0] = true;
            boolean changed = model.update(data -> model.entries()
                    .setAmount(data, habit.id(), model.today(), value.getAsDouble(), minimalDone));
            if (!changed) {
                handled[0] = false;
                field.setText(initial);
            }
        };
        field.setOnAction(event -> commit.run());
        field.focusedProperty().addListener((observable, was, focused) -> {
            if (!focused) {
                commit.run();
            }
        });
    }

    private static Button actionButton(String text, boolean on, String styleClass) {
        Button button = new Button(on ? Messages.get("day.symbol.done") + " " + text : text);
        button.getStyleClass().addAll("action-button", styleClass);
        if (on) {
            button.getStyleClass().add("on");
        }
        return button;
    }

    private void setChecked(Habit habit, boolean checked, boolean minimal) {
        model.update(data -> model.entries().setChecked(data, habit.id(), model.today(), checked, minimal));
    }

    private void addAmount(Habit habit, double delta) {
        model.update(data -> model.entries().addAmount(data, habit.id(), model.today(), delta));
    }
}
