package pl.mojrytm.ui;

import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

/** Lista ekranów aplikacji w kolejności paska nawigacji. */
final class Screens {

    private Screens() {
    }

    static List<Screen> create(AppModel model) {
        return List.of(
                new Screen(Messages.get("nav.today"), scrollable(new TodayScreen(model))),
                new Screen(Messages.get("nav.week"), scrollable(new WeekScreen(model))),
                new Screen(Messages.get("nav.habits"), scrollable(new HabitsScreen(model))),
                new Screen(Messages.get("nav.goals"), scrollable(new GoalsScreen(model))),
                new Screen(Messages.get("nav.counters"), scrollable(new CountersScreen(model))),
                new Screen(Messages.get("nav.pauses"), scrollable(new PausesScreen(model))));
    }

    private static ScrollPane scrollable(Node content) {
        ScrollPane pane = new ScrollPane(content);
        pane.setFitToWidth(true);
        pane.getStyleClass().add("screen-scroll");
        return pane;
    }
}
