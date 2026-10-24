package pl.mojrytm.ui;

import javafx.scene.Node;
import javafx.stage.Window;
import pl.mojrytm.model.Habit;

/** Akcje na nawykach wspólne dla kilku ekranów: dodaj, edytuj, archiwizuj. */
final class HabitActions {

    private HabitActions() {
    }

    static void create(Node anchor, AppModel model) {
        HabitDialog.show(windowOf(anchor), null, model.today())
                .ifPresent(habit -> model.update(data -> model.habits().add(data, habit)));
    }

    static void edit(Node anchor, AppModel model, Habit habit) {
        HabitDialog.show(windowOf(anchor), habit, model.today())
                .ifPresent(updated -> model.update(data -> model.habits().update(data, updated)));
    }

    static void setArchived(AppModel model, Habit habit, boolean archived) {
        model.update(data -> model.habits().setArchived(data, habit.id(), archived));
        if (archived) {
            model.showInfo(Messages.format("habits.archived.info", habit.name()));
        } else {
            model.showInfo(Messages.format("habits.restored.info", habit.name()));
        }
    }

    private static Window windowOf(Node anchor) {
        return anchor.getScene() == null ? null : anchor.getScene().getWindow();
    }
}
