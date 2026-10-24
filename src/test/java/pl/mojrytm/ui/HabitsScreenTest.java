package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.mojrytm.model.AppData;
import pl.mojrytm.storage.JsonStorage;

class HabitsScreenTest {

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock("2026-10-24T10:00:00Z");
    private AppModel model;
    private HabitsScreen screen;

    @BeforeAll
    static void toolkit() {
        FxTestSupport.assumeToolkit();
    }

    @AfterEach
    void tearDown() throws IOException {
        model.shutdown();
    }

    private void open(AppData data) {
        model = new AppModel(data, new JsonStorage(dir, clock), clock);
        screen = FxTestSupport.onFx(() -> new HabitsScreen(model));
    }

    private List<String> texts(String styleClass) {
        return screen.lookupAll(styleClass).stream().map(node -> ((Label) node).getText()).toList();
    }

    private Node row(String name) {
        return screen.lookupAll(".habit-card").stream()
                .filter(card -> ((Label) card.lookup(".habit-name")).getText().equals(name))
                .findFirst().orElseThrow();
    }

    @Test
    void listsActiveHabitsThenArchivedOnesUnderTheirHeadings() {
        open(UiTestData.sample());

        assertEquals(List.of("Aktywne", "Zarchiwizowane"), texts(".section-title"));
        assertEquals(List.of("Spacer", "Woda", "Bieg", "Stary nawyk"), texts(".habit-name"));
        assertTrue(row("Stary nawyk").getStyleClass().contains("archived"));
        assertFalse(row("Spacer").getStyleClass().contains("archived"));
    }

    @Test
    void describesScheduleKindStartAndMinimumOfEachHabit() {
        open(UiTestData.sample());

        assertEquals(List.of(
                "Codziennie · Zaznaczenie · od 01.01.2026 · Minimum: Wyjść przed dom",
                "Codziennie · Ilość: 8 szklanki · od 01.01.2026",
                "3 razy w tygodniu · Zaznaczenie · od 01.01.2026",
                "Codziennie · Zaznaczenie · od 01.01.2026"), texts(".habit-meta"));
    }

    @Test
    void archivingMovesTheHabitAndKeepsItsHistory() {
        open(UiTestData.sample());

        FxTestSupport.onFx(((Button) row("Woda").lookup(".archive-button"))::fire);

        assertTrue(model.data().habits().stream().filter(h -> h.id().equals("woda")).findFirst().orElseThrow().archived());
        assertEquals(1, model.data().entries().stream().filter(e -> e.habitId().equals("woda")).count());
        assertTrue(row("Woda").getStyleClass().contains("archived"));
        assertEquals(new StatusMessage(
                "Zarchiwizowano: Woda. Historia zostaje, możesz go przywrócić w zakładce Nawyki.", false),
                model.statusProperty().get());
    }

    @Test
    void restoringBringsTheHabitBackToTheActiveList() {
        open(UiTestData.sample());

        FxTestSupport.onFx(((Button) row("Stary nawyk").lookup(".archive-button"))::fire);

        assertFalse(row("Stary nawyk").getStyleClass().contains("archived"));
        assertEquals(List.of("Aktywne"), texts(".section-title"));
        assertEquals("Przywrócono: Stary nawyk", ((StatusMessage) model.statusProperty().get()).text());
    }

    @Test
    void archiveButtonLabelDependsOnTheState() {
        open(UiTestData.sample());

        assertEquals("Archiwizuj", ((Button) row("Spacer").lookup(".archive-button")).getText());
        assertEquals("Przywróć", ((Button) row("Stary nawyk").lookup(".archive-button")).getText());
    }

    @Test
    void withoutHabitsShowsAHint() {
        open(AppData.empty());

        assertEquals(List.of("Nie ma jeszcze żadnych nawyków."), texts(".habit-meta"));
    }
}
