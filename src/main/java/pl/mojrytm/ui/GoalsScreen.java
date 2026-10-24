package pl.mojrytm.ui;

import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pl.mojrytm.model.Area;
import pl.mojrytm.model.Goal;
import pl.mojrytm.service.AreaNode;
import pl.mojrytm.service.GoalNode;

/**
 * Ekran „Cele": obszary życia i ich cele (Obszar → Cel). Nawyki przypisuje się do celu w oknie
 * nawyku, tu widać tylko, ile nawyków ma każdy cel. Archiwizacja niczego nie kasuje.
 */
final class GoalsScreen extends VBox {

    private final AppModel model;

    GoalsScreen(AppModel model) {
        super(12);
        this.model = model;
        getStyleClass().add("screen-content");
        model.dataProperty().addListener((observable, previous, current) -> rebuild());
        rebuild();
    }

    private void rebuild() {
        getChildren().clear();
        Label title = new Label(Messages.get("goals.title"));
        title.getStyleClass().add("screen-title");
        Label intro = new Label(Messages.get("goals.intro"));
        intro.getStyleClass().add("habit-meta");
        intro.setWrapText(true);
        getChildren().addAll(title, intro);

        List<AreaNode> areas = model.goalBoard().areas(model.data());
        Button create = new Button(Messages.get("goals.area.new"));
        create.getStyleClass().addAll("primary-button", "new-area-button");
        create.setOnAction(event -> GoalActions.createArea(this, model));
        HBox buttons = new HBox(12, create);
        if (areas.isEmpty()) {
            Button defaults = new Button(Messages.get("goals.area.defaults"));
            defaults.getStyleClass().add("defaults-button");
            defaults.setOnAction(event -> GoalActions.addDefaultAreas(model));
            buttons.getChildren().add(defaults);
        }
        getChildren().add(buttons);
        if (areas.isEmpty()) {
            Label empty = new Label(Messages.get("goals.empty"));
            empty.getStyleClass().add("habit-meta");
            getChildren().add(empty);
            return;
        }

        areas.stream().filter(node -> !node.area().archived()).forEach(node -> getChildren().add(areaCard(node)));
        List<AreaNode> archived = areas.stream().filter(node -> node.area().archived()).toList();
        if (!archived.isEmpty()) {
            Label heading = new Label(Messages.get("goals.areas.archived"));
            heading.getStyleClass().add("section-title");
            getChildren().add(heading);
            archived.forEach(node -> getChildren().add(areaCard(node)));
        }
    }

    private Node areaCard(AreaNode node) {
        Area area = node.area();
        Label name = new Label(area.name());
        name.getStyleClass().add("area-name");
        HBox.setHgrow(name, Priority.ALWAYS);
        name.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox(8, name);
        header.setAlignment(Pos.CENTER_LEFT);
        if (!area.archived()) {
            Button addGoal = new Button(Messages.get("goals.area.addGoal"));
            addGoal.getStyleClass().add("add-goal-button");
            addGoal.setOnAction(event -> GoalActions.createGoal(this, model, area));
            header.getChildren().add(addGoal);
        }
        Button rename = new Button(Messages.get("goals.rename"));
        rename.getStyleClass().addAll("edit-button", "rename-area-button");
        rename.setOnAction(event -> GoalActions.renameArea(this, model, area));
        Button toggleArchive = new Button(Messages.get(area.archived() ? "goals.restore" : "goals.archive"));
        toggleArchive.getStyleClass().addAll("archive-button", "archive-area-button");
        toggleArchive.setOnAction(event -> GoalActions.setAreaArchived(model, area, !area.archived()));
        header.getChildren().addAll(rename, toggleArchive);

        VBox card = new VBox(8, header);
        card.getStyleClass().add("area-card");
        if (area.archived()) {
            card.getStyleClass().add("archived");
        }
        if (node.goals().isEmpty()) {
            Label none = new Label(Messages.get("goals.area.noGoals"));
            none.getStyleClass().add("habit-meta");
            card.getChildren().add(none);
        }
        node.goals().forEach(goal -> card.getChildren().add(goalRow(goal, area.archived())));
        return card;
    }

    private Node goalRow(GoalNode node, boolean areaArchived) {
        Goal goal = node.goal();
        Label name = new Label(goal.name());
        name.getStyleClass().add("goal-name");
        Label habits = new Label(Formats.habitCount(node.activeHabits()));
        habits.getStyleClass().add("habit-meta");
        VBox info = new VBox(2, name, habits);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox row = new HBox(8, info);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("goal-row");
        if (goal.archived()) {
            row.getStyleClass().add("archived");
        }
        if (!areaArchived) {
            Button rename = new Button(Messages.get("goals.rename"));
            rename.getStyleClass().addAll("edit-button", "rename-goal-button");
            rename.setOnAction(event -> GoalActions.renameGoal(this, model, goal));
            Button toggleArchive = new Button(Messages.get(goal.archived() ? "goals.restore" : "goals.archive"));
            toggleArchive.getStyleClass().addAll("archive-button", "archive-goal-button");
            toggleArchive.setOnAction(event -> GoalActions.setGoalArchived(model, goal, !goal.archived()));
            row.getChildren().addAll(rename, toggleArchive);
        }
        return row;
    }
}
