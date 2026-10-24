package pl.mojrytm.ui;

import java.util.List;
import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

/** Główne okno: pasek nawigacji, wybrany ekran i pasek stanu z krótkimi komunikatami. */
final class MainView extends BorderPane {

    private static final Duration STATUS_TIMEOUT = Duration.seconds(8);

    private final AppModel model;
    private Screen selected;
    private final Label statusLabel = new Label();
    private final PauseTransition statusTimeout = new PauseTransition(STATUS_TIMEOUT);

    MainView(AppModel model, List<Screen> screens) {
        this.model = model;
        getStyleClass().add("main-view");
        setTop(buildNavigation(screens));
        setBottom(buildStatusBar());
    }

    private HBox buildNavigation(List<Screen> screens) {
        ToggleGroup group = new ToggleGroup();
        HBox navigation = new HBox();
        navigation.getStyleClass().add("navigation");
        for (Screen screen : screens) {
            ToggleButton button = new ToggleButton(screen.title());
            button.getStyleClass().add("nav-button");
            button.setToggleGroup(group);
            button.setUserData(screen);
            navigation.getChildren().add(button);
        }
        // zawsze jakiś ekran jest wybrany: kliknięcie w aktywny przycisk niczego nie odznacza
        group.selectedToggleProperty().addListener((observable, previous, current) -> {
            if (current == null && previous != null) {
                previous.setSelected(true);
            } else if (current != null) {
                selected = (Screen) current.getUserData();
                setCenter(selected.content());
            }
        });
        if (!group.getToggles().isEmpty()) {
            group.selectToggle(group.getToggles().get(0));
        }
        return navigation;
    }

    private HBox buildStatusBar() {
        statusLabel.setWrapText(true);
        HBox bar = new HBox(statusLabel);
        bar.getStyleClass().add("status-bar");
        bar.setVisible(false);
        bar.setManaged(false);
        statusTimeout.setOnFinished(event -> model.clearStatus());
        model.statusProperty().addListener((observable, previous, message) -> {
            statusLabel.getStyleClass().removeAll("status-info", "status-error");
            if (message == null) {
                bar.setVisible(false);
                bar.setManaged(false);
                return;
            }
            statusLabel.setText(message.text());
            statusLabel.getStyleClass().add(message.error() ? "status-error" : "status-info");
            bar.setVisible(true);
            bar.setManaged(true);
            statusTimeout.playFromStart();
        });
        return bar;
    }

    /** Wybrany teraz ekran. */
    Screen selectedScreen() {
        return selected;
    }
}
