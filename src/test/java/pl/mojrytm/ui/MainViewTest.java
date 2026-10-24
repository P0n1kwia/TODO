package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.mojrytm.model.AppData;
import pl.mojrytm.storage.JsonStorage;

class MainViewTest {

    @TempDir
    Path dir;

    private AppModel model;
    private Screen first;
    private Screen second;
    private MainView view;

    @BeforeAll
    static void toolkit() {
        FxTestSupport.assumeToolkit();
    }

    @BeforeEach
    void setUp() {
        MutableClock clock = new MutableClock("2026-10-24T10:00:00Z");
        model = new AppModel(AppData.empty(), new JsonStorage(dir, clock), clock);
        first = new Screen("Pierwszy", new StackPane(new Label("a")));
        second = new Screen("Drugi", new StackPane(new Label("b")));
        view = FxTestSupport.onFx(() -> new MainView(model, List.of(first, second)));
    }

    @AfterEach
    void tearDown() throws IOException {
        model.shutdown();
    }

    private List<ToggleButton> buttons() {
        return view.lookupAll(".nav-button").stream().map(node -> (ToggleButton) node).toList();
    }

    @Test
    void showsTheFirstScreenInitially() {
        assertEquals(first, view.selectedScreen());
        assertEquals(first.content(), view.getCenter());
        assertEquals(List.of("Pierwszy", "Drugi"), buttons().stream().map(ToggleButton::getText).toList());
    }

    @Test
    void clickingAnotherButtonSwitchesTheScreen() {
        FxTestSupport.onFx(() -> buttons().get(1).fire());

        assertEquals(second, view.selectedScreen());
        assertEquals(second.content(), view.getCenter());
    }

    @Test
    void clickingTheActiveButtonDoesNotLeaveNothingSelected() {
        FxTestSupport.onFx(() -> buttons().get(0).fire());

        assertEquals(first, view.selectedScreen());
        assertTrue(buttons().get(0).isSelected());
    }

    @Test
    void statusBarShowsAndHidesMessages() {
        var bar = view.lookup(".status-bar");
        assertFalse(bar.isVisible());

        FxTestSupport.onFx(() -> model.showError("Coś poszło nie tak"));
        Label label = (Label) view.lookup(".status-error");

        assertTrue(bar.isVisible());
        assertEquals("Coś poszło nie tak", label.getText());

        FxTestSupport.onFx(model::clearStatus);
        assertFalse(bar.isVisible());
    }
}
