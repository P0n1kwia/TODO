package pl.mojrytm.ui;

import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.model.Ids;

/** Okno dodawania i edycji nawyku. Walidację i budowanie nawyku robi {@link HabitForm}. */
final class HabitDialog {

    private HabitDialog() {
    }

    /**
     * @param existing nawyk do edycji (rodzaj i dzień założenia są wtedy zablokowane) albo null dla nowego
     * @param today    domyślny dzień rozpoczęcia nowego nawyku
     */
    static Optional<Habit> show(Window owner, Habit existing, LocalDate today) {
        boolean editing = existing != null;
        HabitForm initial = editing
                ? HabitForm.from(existing)
                : new HabitForm("", HabitKind.CHECK, false, 3, "", "", "", today);

        Dialog<Habit> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(editing ? Messages.get("dialog.habit.edit.title") : Messages.get("dialog.habit.new.title"));
        ButtonType save = new ButtonType(Messages.get("dialog.habit.save"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(Messages.get("dialog.habit.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(save, cancel);

        TextField name = new TextField(initial.name());
        name.setPromptText(Messages.get("dialog.habit.name.prompt"));
        name.setPrefColumnCount(32);

        ToggleGroup kindGroup = new ToggleGroup();
        RadioButton checkKind = new RadioButton(Messages.get("dialog.habit.kind.check"));
        RadioButton countKind = new RadioButton(Messages.get("dialog.habit.kind.count"));
        checkKind.setToggleGroup(kindGroup);
        countKind.setToggleGroup(kindGroup);
        (initial.kind() == HabitKind.COUNT ? countKind : checkKind).setSelected(true);
        checkKind.setDisable(editing);
        countKind.setDisable(editing);

        TextField target = new TextField(initial.targetText());
        TextField unit = new TextField(initial.unit());
        unit.setPromptText(Messages.get("dialog.habit.unit.prompt"));

        ToggleGroup scheduleGroup = new ToggleGroup();
        RadioButton daily = new RadioButton(Messages.get("dialog.habit.schedule.daily"));
        RadioButton weekly = new RadioButton(Messages.get("dialog.habit.schedule.weekly"));
        daily.setToggleGroup(scheduleGroup);
        weekly.setToggleGroup(scheduleGroup);
        (initial.weekly() ? weekly : daily).setSelected(true);
        Spinner<Integer> times = new Spinner<>(1, 7, initial.timesPerWeek());
        times.setPrefWidth(80);
        times.disableProperty().bind(weekly.selectedProperty().not());

        TextField minimum = new TextField(initial.minimumVersion());
        minimum.setPromptText(Messages.get("dialog.habit.minimum.prompt"));
        minimum.setPrefColumnCount(32);

        DatePicker start = new DatePicker(initial.start());
        start.setConverter(new DateConverter());
        start.setDisable(editing);

        Label problem = new Label();
        problem.getStyleClass().add("form-problem");
        problem.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.setMinWidth(520);
        int row = 0;
        grid.addRow(row++, new Label(Messages.get("dialog.habit.name")), name);
        grid.addRow(row++, new Label(Messages.get("dialog.habit.kind")), new VBox(6, checkKind, countKind));
        Node targetLabel = new Label(Messages.get("dialog.habit.target"));
        HBox targetBox = new HBox(8, target, unit);
        grid.addRow(row++, targetLabel, targetBox);
        targetLabel.visibleProperty().bind(countKind.selectedProperty());
        targetLabel.managedProperty().bind(countKind.selectedProperty());
        targetBox.visibleProperty().bind(countKind.selectedProperty());
        targetBox.managedProperty().bind(countKind.selectedProperty());
        grid.addRow(row++, new Label(Messages.get("dialog.habit.schedule")),
                new VBox(6, daily, new HBox(8, weekly, times)));
        grid.addRow(row++, new Label(Messages.get("dialog.habit.minimum")), minimum);
        grid.addRow(row++, new Label(Messages.get("dialog.habit.start")), start);
        grid.add(problem, 1, row);
        dialog.getDialogPane().setContent(grid);

        Supplier<HabitForm> form = () -> new HabitForm(name.getText(),
                countKind.isSelected() ? HabitKind.COUNT : HabitKind.CHECK, weekly.isSelected(), times.getValue(),
                target.getText(), unit.getText(), minimum.getText(), start.getValue());

        Node saveButton = dialog.getDialogPane().lookupButton(save);
        Runnable refresh = () -> {
            Optional<HabitForm.Problem> found = form.get().problem();
            saveButton.setDisable(found.isPresent());
            problem.setText(found.map(p -> Messages.get("habit.form.problem." + p.name())).orElse(""));
        };
        name.textProperty().addListener((o, a, b) -> refresh.run());
        target.textProperty().addListener((o, a, b) -> refresh.run());
        kindGroup.selectedToggleProperty().addListener((o, a, b) -> refresh.run());
        start.valueProperty().addListener((o, a, b) -> refresh.run());
        refresh.run();

        dialog.setResultConverter(button -> button == save
                ? form.get().toHabit(editing ? existing.id() : Ids.newId(),
                        editing ? existing.goalId() : null, editing && existing.archived())
                : null);
        Platform.runLater(name::requestFocus);
        return dialog.showAndWait();
    }
}
