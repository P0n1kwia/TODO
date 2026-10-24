package pl.mojrytm.ui;

import java.util.Arrays;
import java.util.List;
import javafx.scene.Node;
import pl.mojrytm.model.Area;
import pl.mojrytm.model.Goal;
import pl.mojrytm.model.Ids;

/** Akcje na obszarach i celach: dodaj, zmień nazwę, archiwizuj, przywróć. */
final class GoalActions {

    private GoalActions() {
    }

    static void createArea(Node anchor, AppModel model) {
        NameDialog.show(Windows.of(anchor), Messages.get("dialog.name.area.new.title"),
                        Messages.get("dialog.name.area.prompt"), "",
                        name -> model.areas().nameTaken(model.data(), name, null))
                .ifPresent(name -> model.update(data -> model.areas().add(data, new Area(Ids.newId(), name, false))));
    }

    static void renameArea(Node anchor, AppModel model, Area area) {
        NameDialog.show(Windows.of(anchor), Messages.get("dialog.name.area.rename.title"),
                        Messages.get("dialog.name.area.prompt"), area.name(),
                        name -> model.areas().nameTaken(model.data(), name, area.id()))
                .filter(name -> !name.equals(area.name()))
                .ifPresent(name -> model.update(data -> model.areas().rename(data, area.id(), name)));
    }

    static void setAreaArchived(AppModel model, Area area, boolean archived) {
        model.update(data -> model.areas().setArchived(data, area.id(), archived));
        model.showInfo(Messages.format(archived ? "goals.area.archived.info" : "goals.area.restored.info",
                area.name()));
    }

    /** Dodaje przykładowe obszary (nazwy z pliku tekstów), pomijając te, które już są. */
    static void addDefaultAreas(AppModel model) {
        List<String> names = Arrays.asList(Messages.get("goals.area.defaults.names").split(";"));
        model.update(data -> model.areas().addAll(data, names));
        model.showInfo(Messages.format("goals.area.defaults.info", String.join(", ", names)));
    }

    static void createGoal(Node anchor, AppModel model, Area area) {
        NameDialog.show(Windows.of(anchor), Messages.format("dialog.name.goal.new.title", area.name()),
                        Messages.get("dialog.name.goal.prompt"), "",
                        name -> model.goals().nameTaken(model.data(), area.id(), name, null))
                .ifPresent(name -> model.update(data ->
                        model.goals().add(data, new Goal(Ids.newId(), area.id(), name, false))));
    }

    static void renameGoal(Node anchor, AppModel model, Goal goal) {
        NameDialog.show(Windows.of(anchor), Messages.get("dialog.name.goal.rename.title"),
                        Messages.get("dialog.name.goal.prompt"), goal.name(),
                        name -> model.goals().nameTaken(model.data(), goal.areaId(), name, goal.id()))
                .filter(name -> !name.equals(goal.name()))
                .ifPresent(name -> model.update(data -> model.goals().rename(data, goal.id(), name)));
    }

    static void setGoalArchived(AppModel model, Goal goal, boolean archived) {
        model.update(data -> model.goals().setArchived(data, goal.id(), archived));
        model.showInfo(Messages.format(archived ? "goals.goal.archived.info" : "goals.goal.restored.info",
                goal.name()));
    }
}
