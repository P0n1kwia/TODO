package pl.mojrytm.ui;

import java.time.LocalDate;
import java.util.List;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.service.DayCell;
import pl.mojrytm.service.DayStatus;
import pl.mojrytm.service.WeekRow;
import pl.mojrytm.service.Weeks;

/**
 * Ekran „Tydzień": siatka nawyki × dni (poniedziałek–niedziela) z wykonaniem tygodnia i serią.
 * Kliknięcie kratki zmienia wpis tego dnia, więc można uzupełniać przeszłe dni; dni z przyszłości
 * i sprzed założenia nawyku są zablokowane.
 */
final class WeekScreen extends VBox {

    private final AppModel model;
    /** 0 = bieżący tydzień, wartości ujemne = wcześniejsze tygodnie. */
    private int weekOffset;

    WeekScreen(AppModel model) {
        super(12);
        this.model = model;
        getStyleClass().add("screen-content");
        model.dataProperty().addListener((observable, previous, current) -> rebuild());
        model.todayProperty().addListener((observable, previous, current) -> rebuild());
        rebuild();
    }

    private void rebuild() {
        getChildren().clear();
        LocalDate start = Weeks.startOf(model.today()).plusWeeks(weekOffset);

        Label title = new Label(Messages.get("week.title"));
        title.getStyleClass().add("screen-title");
        getChildren().addAll(title, navigation(start));

        List<WeekRow> rows = model.board().weekRows(model.data(), start, model.today());
        if (rows.isEmpty()) {
            Label empty = new Label(Messages.get("week.empty"));
            empty.getStyleClass().add("habit-meta");
            getChildren().add(empty);
            return;
        }
        getChildren().add(grid(start, rows));
        Label legend = new Label(Messages.get("week.legend"));
        legend.getStyleClass().add("habit-meta");
        legend.setWrapText(true);
        getChildren().add(legend);
    }

    private Node navigation(LocalDate start) {
        Button previous = new Button(Messages.get("week.previous"));
        previous.getStyleClass().add("previous-week");
        previous.setOnAction(event -> shift(-1));
        Button current = new Button(Messages.get("week.current"));
        current.getStyleClass().add("current-week");
        current.setDisable(weekOffset == 0);
        current.setOnAction(event -> {
            weekOffset = 0;
            rebuild();
        });
        Button next = new Button(Messages.get("week.next"));
        next.getStyleClass().add("next-week");
        next.setDisable(weekOffset == 0);
        next.setOnAction(event -> shift(1));
        Label range = new Label(Formats.weekRange(start));
        range.getStyleClass().addAll("screen-subtitle", "week-range");
        range.setMinWidth(260);
        range.setAlignment(Pos.CENTER);

        HBox bar = new HBox(8, previous, range, next, current);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void shift(int weeks) {
        weekOffset = Math.min(0, weekOffset + weeks);
        rebuild();
    }

    private Node grid(LocalDate start, List<WeekRow> rows) {
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        ColumnConstraints nameColumn = new ColumnConstraints(190, 240, Double.MAX_VALUE);
        nameColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().add(nameColumn);
        for (int i = 0; i < 7; i++) {
            grid.getColumnConstraints().add(new ColumnConstraints(54));
        }
        grid.getColumnConstraints().add(new ColumnConstraints(110));
        grid.getColumnConstraints().add(new ColumnConstraints(130));

        for (int i = 0; i < 7; i++) {
            LocalDate day = start.plusDays(i);
            Label header = new Label(dayName(i) + "\n" + day.getDayOfMonth());
            header.getStyleClass().add("day-header");
            if (day.equals(model.today())) {
                header.getStyleClass().add("today");
            }
            header.setAlignment(Pos.CENTER);
            header.setMaxWidth(Double.MAX_VALUE);
            grid.add(header, i + 1, 0);
            GridPane.setHalignment(header, HPos.CENTER);
        }
        grid.add(columnTitle(Messages.get("week.progress")), 8, 0);
        grid.add(columnTitle(Messages.get("week.streak")), 9, 0);

        int rowIndex = 1;
        for (WeekRow row : rows) {
            addRow(grid, rowIndex++, row);
        }
        return grid;
    }

    private static Label columnTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("day-header");
        return label;
    }

    private void addRow(GridPane grid, int rowIndex, WeekRow row) {
        Habit habit = row.habit();
        Label name = new Label(habit.name());
        name.getStyleClass().add("habit-name");
        Label meta = new Label(Formats.schedule(habit));
        meta.getStyleClass().add("habit-meta");
        grid.add(new VBox(2, name, meta), 0, rowIndex);

        List<DayCell> days = row.week().days();
        for (int i = 0; i < days.size(); i++) {
            grid.add(dayButton(habit, days.get(i)), i + 1, rowIndex);
        }

        Label progress = new Label(Formats.progress(row.week().progress()));
        progress.getStyleClass().add("week-progress");
        if (row.week().progress().completed()) {
            progress.getStyleClass().add("completed");
        }
        grid.add(progress, 8, rowIndex);
        Label streak = new Label(Formats.streak(row.streak()));
        streak.getStyleClass().add("streak-label");
        grid.add(streak, 9, rowIndex);
    }

    private Button dayButton(Habit habit, DayCell cell) {
        Button button = new Button(cellText(habit, cell));
        button.setId("cell-" + habit.id() + "-" + cell.date());
        button.getStyleClass().add("day-cell");
        button.setMinSize(48, 40);
        button.setPrefSize(48, 40);
        if (cell.status() == DayStatus.DONE) {
            button.getStyleClass().add("done");
        } else if (cell.status() == DayStatus.DONE_MINIMAL) {
            button.getStyleClass().add("minimal");
        }
        if (cell.paused()) {
            button.getStyleClass().add("paused");
        }
        if (!cell.editable()) {
            button.getStyleClass().add("unavailable");
            button.setDisable(true);
            return button;
        }
        button.setTooltip(new Tooltip(Formats.longDate(cell.date()) + ": " + describe(cell)));
        button.setOnAction(event -> click(habit, cell));
        return button;
    }

    /** CHECK pokazuje symbol, COUNT ilość (a gdy jej nie ma, symbol wersji minimalnej). */
    private static String cellText(Habit habit, DayCell cell) {
        if (habit.kind() == HabitKind.COUNT && cell.amount() > 0) {
            return Formats.amount(cell.amount());
        }
        return Formats.symbol(cell.status());
    }

    private static String describe(DayCell cell) {
        String state = switch (cell.status()) {
            case DONE -> Messages.get("week.state.done");
            case DONE_MINIMAL -> Messages.get("week.state.minimal");
            case NOT_DONE -> Messages.get("week.state.none");
        };
        return cell.paused() ? state + " · " + Messages.get("week.state.paused") : state;
    }

    private void click(Habit habit, DayCell cell) {
        if (habit.kind() == HabitKind.COUNT) {
            AmountDialog.show(getScene() == null ? null : getScene().getWindow(), habit, cell.date(), cell.amount(),
                    cell.status() == DayStatus.DONE_MINIMAL)
                    .ifPresent(choice -> model.update(data -> model.entries()
                            .setAmount(data, habit.id(), cell.date(), choice.amount(), choice.minimal())));
            return;
        }
        // CHECK: puste -> zrobione -> (wersja minimalna, jeśli nawyk ją ma) -> puste
        boolean hasMinimum = habit.minimumVersion() != null;
        switch (cell.status()) {
            case NOT_DONE -> setChecked(habit, cell, true, false);
            case DONE -> setChecked(habit, cell, hasMinimum, hasMinimum);
            case DONE_MINIMAL -> setChecked(habit, cell, false, false);
        }
    }

    private void setChecked(Habit habit, DayCell cell, boolean checked, boolean minimal) {
        model.update(data -> model.entries().setChecked(data, habit.id(), cell.date(), checked, minimal));
    }

    private static String dayName(int index) {
        return switch (index) {
            case 0 -> Messages.get("week.day.mon");
            case 1 -> Messages.get("week.day.tue");
            case 2 -> Messages.get("week.day.wed");
            case 3 -> Messages.get("week.day.thu");
            case 4 -> Messages.get("week.day.fri");
            case 5 -> Messages.get("week.day.sat");
            default -> Messages.get("week.day.sun");
        };
    }
}
