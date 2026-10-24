package pl.mojrytm.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pl.mojrytm.model.Habit;
import pl.mojrytm.service.HabitGroup;

/** Ekran „Nawyki": lista wszystkich nawyków z edycją, archiwizacją i przywracaniem. */
final class HabitsScreen extends VBox {

    private final AppModel model;

    HabitsScreen(AppModel model) {
        super(12);
        this.model = model;
        getStyleClass().add("screen-content");
        model.dataProperty().addListener((observable, previous, current) -> rebuild());
        rebuild();
    }

    private void rebuild() {
        getChildren().clear();
        Label title = new Label(Messages.get("habits.title"));
        title.getStyleClass().add("screen-title");
        Button create = new Button(Messages.get("habits.new"));
        create.getStyleClass().add("primary-button");
        create.setOnAction(event -> HabitActions.create(this, model));
        getChildren().addAll(title, create);

        List<HabitGroup> groups = model.goalBoard().habitGroups(model.data());
        List<Habit> archived = model.data().habits().stream().filter(Habit::archived).toList();
        if (groups.isEmpty() && archived.isEmpty()) {
            Label empty = new Label(Messages.get("habits.empty"));
            empty.getStyleClass().add("habit-meta");
            getChildren().add(empty);
            return;
        }
        if (!groups.isEmpty()) {
            getChildren().add(sectionTitle(Messages.get("habits.active")));
            // nagłówki grup tylko wtedy, gdy jakiś nawyk ma cel; inaczej jedna lista jak dotąd
            boolean grouped = groups.stream().anyMatch(group -> group.goal() != null);
            for (HabitGroup group : groups) {
                if (grouped) {
                    getChildren().add(groupTitle(group));
                }
                group.habits().forEach(habit -> getChildren().add(row(habit)));
            }
        }
        addSection(Messages.get("habits.archived"), archived);
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private static Label groupTitle(HabitGroup group) {
        Label label = new Label(group.goal() == null
                ? Messages.get("habits.group.none")
                : Formats.goalLabel(group.goal()));
        label.getStyleClass().add("group-title");
        return label;
    }

    private void addSection(String heading, List<Habit> habits) {
        if (habits.isEmpty()) {
            return;
        }
        getChildren().add(sectionTitle(heading));
        habits.forEach(habit -> getChildren().add(row(habit)));
    }

    private Node row(Habit habit) {
        Label name = new Label(habit.name());
        name.getStyleClass().add("habit-name");
        List<String> parts = new ArrayList<>(List.of(Formats.schedule(habit), Formats.kind(habit),
                Messages.format("habit.since", Formats.numericDate(habit.created()))));
        if (habit.minimumVersion() != null) {
            parts.add(Messages.format("habit.minimum", habit.minimumVersion()));
        }
        if (habit.archived()) {
            // aktywne nawyki stoją pod nagłówkiem swojego celu, zarchiwizowane tworzą jedną listę
            model.goalBoard().goalOf(model.data(), habit.goalId())
                    .ifPresent(goal -> parts.add(Messages.format("habit.goal", Formats.goalLabel(goal))));
        }
        Label details = new Label(String.join(" · ", parts));
        details.getStyleClass().add("habit-meta");
        details.setWrapText(true);
        VBox info = new VBox(2, name, details);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button edit = new Button(Messages.get("habits.edit"));
        edit.getStyleClass().add("edit-button");
        edit.setOnAction(event -> HabitActions.edit(this, model, habit));
        Button toggleArchive = habit.archived()
                ? new Button(Messages.get("habits.restore"))
                : new Button(Messages.get("habits.archive"));
        toggleArchive.getStyleClass().add("archive-button");
        toggleArchive.setOnAction(event -> HabitActions.setArchived(model, habit, !habit.archived()));

        HBox row = new HBox(12, info, edit, toggleArchive);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("habit-card");
        if (habit.archived()) {
            row.getStyleClass().add("archived");
        }
        return row;
    }
}
