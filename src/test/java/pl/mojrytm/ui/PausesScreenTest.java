package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Pause;
import pl.mojrytm.storage.JsonStorage;

class PausesScreenTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock("2026-10-24T10:00:00Z");
    private AppModel model;
    private PausesScreen screen;

    @BeforeAll
    static void toolkit() {
        FxTestSupport.assumeToolkit();
    }

    @BeforeEach
    void setUp() {
        AppData data = AppData.empty().withPauses(List.of(
                new Pause(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3), "choroba"),
                new Pause(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 12), null),
                new Pause(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), "wyjazd")));
        model = new AppModel(data, new JsonStorage(dir, clock), clock);
        screen = FxTestSupport.onFx(() -> new PausesScreen(model));
    }

    @AfterEach
    void tearDown() throws IOException {
        model.shutdown();
    }

    private DatePicker picker(String id) {
        return (DatePicker) screen.lookup("#" + id);
    }

    private Button addButton() {
        return (Button) screen.lookup("#pause-add");
    }

    private List<String> ranges() {
        return screen.lookupAll(".habit-card .habit-name").stream().map(n -> ((Label) n).getText()).toList();
    }

    @Test
    void listsPausesNewestFirstWithReasons() {
        assertEquals(List.of("12.10.2026", "01.10.2026 – 05.10.2026", "01.09.2026 – 03.09.2026"), ranges());
        List<String> reasons = screen.lookupAll(".habit-card .habit-meta").stream()
                .map(n -> ((Label) n).getText()).toList();
        assertEquals(List.of("wyjazd", "choroba"), reasons);
    }

    @Test
    void theFormStartsOnTodayAndIsReadyToAdd() {
        assertEquals(TODAY, picker("pause-from").getValue());
        assertEquals(TODAY, picker("pause-to").getValue());
        assertFalse(addButton().isDisabled());
    }

    @Test
    void addingAPauseStoresItAndShowsItInTheList() {
        FxTestSupport.onFx(() -> {
            picker("pause-from").setValue(TODAY.plusDays(5));
            picker("pause-to").setValue(TODAY.plusDays(7));
            ((ComboBox<?>) screen.lookup("#pause-reason")).getEditor().setText("wyjazd");
            addButton().fire();
        });

        assertTrue(model.data().pauses().contains(new Pause(TODAY.plusDays(5), TODAY.plusDays(7), "wyjazd")));
        assertEquals("29.10.2026 – 31.10.2026", ranges().get(0));
        assertEquals(new StatusMessage("Dodano pauzę: 29.10.2026 – 31.10.2026", false), model.statusProperty().get());
        assertEquals(TODAY, picker("pause-from").getValue(), "formularz wraca do stanu początkowego");
    }

    @Test
    void anEndBeforeTheStartDisablesAddingAndExplainsWhy() {
        FxTestSupport.onFx(() -> picker("pause-to").setValue(TODAY.minusDays(1)));

        assertTrue(addButton().isDisabled());
        assertEquals("Koniec pauzy nie może być przed jej początkiem.",
                ((Label) screen.lookup(".form-problem")).getText());
        assertEquals(3, model.data().pauses().size());
    }

    @Test
    void aMissingDateDisablesAdding() {
        FxTestSupport.onFx(() -> picker("pause-from").setValue(null));

        assertTrue(addButton().isDisabled());
    }

    @Test
    void removingAPauseDeletesItFromTheData() {
        Button remove = (Button) screen.lookupAll(".remove-button").iterator().next();

        FxTestSupport.onFx(remove::fire);

        assertEquals(2, model.data().pauses().size());
        assertFalse(model.data().pauses().contains(new Pause(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 12), null)));
        assertEquals(new StatusMessage("Usunięto pauzę: 12.10.2026", false), model.statusProperty().get());
    }

    @Test
    void withoutPausesShowsAHint() {
        FxTestSupport.onFx(() -> model.update(data -> data.withPauses(List.of())));

        assertEquals(List.of(), ranges());
        assertTrue(screen.lookupAll(".habit-meta").stream().anyMatch(n -> ((Label) n).getText().equals("Nie masz żadnych pauz.")));
    }
}
