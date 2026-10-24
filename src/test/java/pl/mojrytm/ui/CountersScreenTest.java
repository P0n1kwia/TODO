package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Counter;
import pl.mojrytm.storage.JsonStorage;

class CountersScreenTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock("2026-10-24T10:00:00Z");
    private final List<String> questions = new ArrayList<>();
    private boolean answer = true;
    private AppModel model;
    private CountersScreen screen;

    @BeforeAll
    static void toolkit() {
        FxTestSupport.assumeToolkit();
    }

    @BeforeEach
    void setUp() {
        AppData data = AppData.empty().withCounters(List.of(
                new Counter("slodycze", "Dni bez słodyczy", LocalDate.of(2026, 9, 1)),
                new Counter("wczoraj", "Od wczoraj", TODAY.minusDays(1)),
                new Counter("dzis", "Od dziś", TODAY)));
        model = new AppModel(data, new JsonStorage(dir, clock), clock);
        screen = FxTestSupport.onFx(() -> new CountersScreen(model, question -> {
            questions.add(question);
            return answer;
        }));
    }

    @AfterEach
    void tearDown() throws IOException {
        model.shutdown();
    }

    private List<String> texts(String selector) {
        return screen.lookupAll(selector).stream().map(node -> ((Label) node).getText()).toList();
    }

    private Node card(String name) {
        return screen.lookupAll(".counter-card").stream()
                .filter(card -> ((Label) card.lookup(".habit-name")).getText().equals(name))
                .findFirst().orElseThrow();
    }

    private static void click(Node button) {
        FxTestSupport.onFx(((Button) button)::fire);
    }

    private Button addButton() {
        return (Button) screen.lookup("#counter-add");
    }

    private TextField nameField() {
        return (TextField) screen.lookup("#counter-name");
    }

    private DatePicker startPicker() {
        return (DatePicker) screen.lookup("#counter-start");
    }

    private Counter stored(String id) {
        return model.data().counters().stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void showsFullDaysSinceTheStartWithTheRightPluralAndTheStartDate() {
        assertEquals(List.of("53", "1", "0"), texts(".counter-days"));
        assertEquals(List.of("dni", "dzień", "dni"), screen.lookupAll(".counter-days").stream()
                .map(days -> ((Label) ((javafx.scene.layout.VBox) days.getParent()).getChildren().get(1)).getText())
                .toList());
    }

    @Test
    void showsTheNameAndStartOfEachCounterInOrder() {
        assertEquals(List.of("Dni bez słodyczy", "Od wczoraj", "Od dziś"), texts(".counter-card .habit-name"));
        assertEquals(List.of("od 01.09.2026", "od 23.10.2026", "od 24.10.2026"),
                texts(".counter-since"));
    }

    @Test
    void aCounterThatStartsTodayCannotBeRestarted() {
        assertTrue(((Button) card("Od dziś").lookup(".restart-button")).isDisabled());
        assertFalse(((Button) card("Od wczoraj").lookup(".restart-button")).isDisabled());
    }

    @Test
    void restartingAsksForConfirmationAndMovesTheStartToToday() {
        click(card("Dni bez słodyczy").lookup(".restart-button"));

        assertEquals(List.of("Zacząć licznik „Dni bez słodyczy” od nowa? Obecny wynik (53 dni) zostanie zastąpiony "
                + "zerem i nie da się go odzyskać."), questions);
        assertEquals(TODAY, stored("slodycze").startDate());
        assertEquals("Dni bez słodyczy", stored("slodycze").name());
        assertEquals("Licznik „Dni bez słodyczy” liczy od nowa, od dziś.", model.statusProperty().get().text());
        assertEquals(List.of("0", "1", "0"), texts(".counter-days"));
    }

    @Test
    void decliningTheRestartChangesNothing() {
        answer = false;

        click(card("Dni bez słodyczy").lookup(".restart-button"));

        assertEquals(LocalDate.of(2026, 9, 1), stored("slodycze").startDate());
        assertEquals(null, model.statusProperty().get());
    }

    @Test
    void removingAsksForConfirmationAndDeletesOnlyThatCounter() {
        click(card("Od wczoraj").lookup(".remove-button"));

        assertEquals(List.of("Usunąć licznik „Od wczoraj”? Tej operacji nie można cofnąć."), questions);
        assertEquals(List.of("slodycze", "dzis"), model.data().counters().stream().map(Counter::id).toList());
        assertEquals("Usunięto licznik: Od wczoraj", model.statusProperty().get().text());
        assertEquals(List.of("Dni bez słodyczy", "Od dziś"), texts(".counter-card .habit-name"));
    }

    @Test
    void decliningTheRemovalKeepsTheCounter() {
        answer = false;

        click(card("Od wczoraj").lookup(".remove-button"));

        assertEquals(3, model.data().counters().size());
    }

    @Test
    void theFormStartsOnTodayAndNeedsAName() {
        assertEquals(TODAY, startPicker().getValue());
        assertTrue(addButton().isDisabled());
        assertEquals("Podaj nazwę licznika.", ((Label) screen.lookup(".form-problem")).getText());
    }

    @Test
    void addingACounterSavesItClearsTheFormAndShowsItInTheList() {
        FxTestSupport.onFx(() -> {
            nameField().setText("  Dni bez kawy ");
            startPicker().setValue(TODAY.minusDays(10));
        });
        assertFalse(addButton().isDisabled());

        click(addButton());

        Counter added = model.data().counters().get(3);
        assertEquals("Dni bez kawy", added.name());
        assertEquals(TODAY.minusDays(10), added.startDate());
        assertEquals("Dodano licznik: Dni bez kawy", model.statusProperty().get().text());
        assertEquals("", nameField().getText());
        assertEquals("10", texts(".counter-days").get(3));
    }

    @Test
    void aStartInTheFutureBlocksAdding() {
        FxTestSupport.onFx(() -> {
            nameField().setText("Bez kawy");
            startPicker().setValue(TODAY.plusDays(1));
        });

        assertTrue(addButton().isDisabled());
        assertEquals("Start nie może być w przyszłości.", ((Label) screen.lookup(".form-problem")).getText());
    }

    @Test
    void countsAreRedrawnWhenTheLogicalDayChanges() {
        clock.advance(Duration.ofHours(24));

        FxTestSupport.onFx(() -> model.refreshToday());

        assertEquals(List.of("54", "2", "1"), texts(".counter-days"));
    }

    @Test
    void withoutCountersShowsAHint() throws IOException {
        model.shutdown();
        model = new AppModel(AppData.empty(), new JsonStorage(dir, clock), clock);
        screen = FxTestSupport.onFx(() -> new CountersScreen(model, question -> true));

        assertEquals(List.of("Nie masz jeszcze żadnych liczników."), texts(".screen-content > .habit-meta").subList(1, 2));
        assertEquals(0, screen.lookupAll(".counter-card").size());
    }
}
