package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.mojrytm.ui.UiTestData.TODAY;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Entry;
import pl.mojrytm.model.Pause;
import pl.mojrytm.storage.JsonStorage;

class TodayScreenTest {

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock("2026-10-24T10:00:00Z");
    private AppModel model;
    private TodayScreen screen;

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
        screen = FxTestSupport.onFx(() -> new TodayScreen(model));
    }

    private List<Node> cards() {
        return screen.lookupAll(".habit-card").stream().toList();
    }

    private Node card(String habitName) {
        return cards().stream()
                .filter(card -> ((Label) card.lookup(".habit-name")).getText().equals(habitName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("brak karty: " + habitName));
    }

    private static List<String> labelTexts(Node card) {
        return card.lookupAll(".label").stream().map(node -> ((Label) node).getText()).toList();
    }

    private static void fire(Node card, String styleClass) {
        Button button = (Button) card.lookup("." + styleClass);
        assertNotNull(button, "brak przycisku " + styleClass);
        FxTestSupport.onFx(button::fire);
    }

    private static void typeAmount(Node card, String text) {
        TextField field = (TextField) card.lookup(".amount-field");
        FxTestSupport.onFx(() -> {
            field.setText(text);
            field.getOnAction().handle(new ActionEvent());
        });
    }

    private List<Entry> entriesOf(String habitId) {
        return model.data().entries().stream().filter(entry -> entry.habitId().equals(habitId)).toList();
    }

    @Test
    void showsACardPerActiveHabitInDataOrder() {
        open(UiTestData.sample());

        List<String> names = cards().stream().map(c -> ((Label) c.lookup(".habit-name")).getText()).toList();

        assertEquals(List.of("Spacer", "Woda", "Bieg"), names);
    }

    @Test
    void showsTheTitleAndTheLogicalDate() {
        open(UiTestData.sample());

        assertEquals("Dziś", ((Label) screen.lookup(".screen-title")).getText());
        assertEquals("Sobota, 24 października 2026", ((Label) screen.lookup(".screen-subtitle")).getText());
    }

    @Test
    void showsStreakAndWeekProgressFromTheService() {
        open(UiTestData.sample());

        List<String> spacer = labelTexts(card("Spacer"));
        List<String> woda = labelTexts(card("Woda"));

        assertTrue(spacer.contains("Seria: 1 dzień"), spacer.toString());
        assertTrue(spacer.contains("Tydzień: 1/6 (17%)"), spacer.toString());
        assertTrue(spacer.contains("Minimum: Wyjść przed dom"), spacer.toString());
        assertTrue(woda.contains("Seria: 0 dni"), woda.toString());
        assertTrue(labelTexts(card("Bieg")).contains("Tydzień: 0/3 (0%)"));
    }

    @Test
    void checkingTodayMarksTheCardDoneAndExtendsTheStreakAndUncheckingUndoesIt() {
        open(UiTestData.sample());

        fire(card("Spacer"), "done-button");

        assertEquals(List.of(new Entry("e1", "spacer", UiTestData.YESTERDAY, 1.0, false)),
                entriesOf("spacer").subList(0, 1));
        assertEquals(2, entriesOf("spacer").size());
        assertTrue(card("Spacer").getStyleClass().contains("done"));
        assertTrue(labelTexts(card("Spacer")).contains("Seria: 2 dni"));
        assertEquals("✓ Zrobione", ((Button) card("Spacer").lookup(".done-button")).getText());

        fire(card("Spacer"), "done-button");

        assertEquals(1, entriesOf("spacer").size());
        assertFalse(card("Spacer").getStyleClass().contains("done"));
        assertTrue(labelTexts(card("Spacer")).contains("Seria: 1 dzień"));
    }

    @Test
    void minimumButtonMarksTheMinimalVersionAndKeepsTheStreakAlive() {
        open(UiTestData.sample());

        fire(card("Spacer"), "minimum-button");

        Entry today = entriesOf("spacer").stream().filter(e -> e.date().equals(TODAY)).findFirst().orElseThrow();
        assertTrue(today.minimal());
        assertTrue(card("Spacer").getStyleClass().contains("minimal"));
        assertTrue(labelTexts(card("Spacer")).contains("Seria: 2 dni"));
    }

    @Test
    void fullDoneReplacesTheMinimalEntryWithoutDuplicates() {
        open(UiTestData.sample());
        fire(card("Spacer"), "minimum-button");

        fire(card("Spacer"), "done-button");

        List<Entry> todayEntries = entriesOf("spacer").stream().filter(e -> e.date().equals(TODAY)).toList();
        assertEquals(1, todayEntries.size());
        assertFalse(todayEntries.get(0).minimal());
        assertTrue(card("Spacer").getStyleClass().contains("done"));
    }

    @Test
    void habitWithoutAMinimumVersionHasNoMinimumButton() {
        open(UiTestData.sample());

        assertNull(card("Bieg").lookup(".minimum-button"));
        assertNull(card("Woda").lookup(".minimum-button"));
    }

    @Test
    void plusAndMinusChangeTheAmountOfACountHabit() {
        open(UiTestData.sample());
        assertEquals("3", ((TextField) card("Woda").lookup(".amount-field")).getText());

        fire(card("Woda"), "plus-button");
        assertEquals("4", ((TextField) card("Woda").lookup(".amount-field")).getText());

        fire(card("Woda"), "minus-button");
        fire(card("Woda"), "minus-button");
        assertEquals("2", ((TextField) card("Woda").lookup(".amount-field")).getText());
        assertEquals(2.0, entriesOf("woda").get(0).value());
    }

    @Test
    void reachingTheTargetMarksTheCountCardDone() {
        open(UiTestData.sample());

        typeAmount(card("Woda"), "7,5");
        assertFalse(card("Woda").getStyleClass().contains("done"));
        fire(card("Woda"), "plus-button");

        assertEquals(8.5, entriesOf("woda").get(0).value());
        assertTrue(card("Woda").getStyleClass().contains("done"));
        assertTrue(labelTexts(card("Woda")).contains("Seria: 1 dzień"));
    }

    @Test
    void anUnreadableAmountShowsAnErrorAndChangesNothing() {
        open(UiTestData.sample());
        AppData before = model.data();

        typeAmount(card("Woda"), "dużo");

        assertEquals(before, model.data());
        assertEquals(new StatusMessage(Messages.get("error.amount.invalid"), true), model.statusProperty().get());
        assertEquals("3", ((TextField) card("Woda").lookup(".amount-field")).getText());
    }

    @Test
    void leavingTheAmountFieldUntouchedDoesNotRoundTheStoredValue() {
        AppData data = UiTestData.sample().withEntries(List.of(new Entry("e", "woda", TODAY, 0.333333, false)));
        open(data);
        TextField field = (TextField) card("Woda").lookup(".amount-field");
        assertEquals("0,333", field.getText());

        FxTestSupport.onFx(() -> field.getOnAction().handle(new ActionEvent()));

        assertEquals(0.333333, entriesOf("woda").get(0).value());
    }

    @Test
    void minimumButtonOnACountHabitMarksTheDayMinimal() {
        AppData data = UiTestData.sample().withHabits(List.of(
                new pl.mojrytm.model.Habit("woda", null, "Woda", pl.mojrytm.model.HabitKind.COUNT,
                        pl.mojrytm.model.Schedule.DAILY, null, 8.0, "szklanki", "Jedna szklanka",
                        UiTestData.LONG_AGO, false)));
        open(data);

        fire(card("Woda"), "minimum-button");

        assertTrue(entriesOf("woda").get(0).minimal());
        assertEquals(3.0, entriesOf("woda").get(0).value());
        assertTrue(card("Woda").getStyleClass().contains("minimal"));

        fire(card("Woda"), "minimum-button");
        assertFalse(entriesOf("woda").get(0).minimal());
    }

    @Test
    void aPauseCoveringTodayShowsTheBannerAndMarksTheCards() {
        open(UiTestData.sample());
        assertNull(screen.lookup(".banner"));

        FxTestSupport.onFx(() -> model.update(data -> model.pauses().add(data, new Pause(TODAY, TODAY, "choroba"))));

        assertNotNull(screen.lookup(".banner"));
        assertTrue(card("Spacer").getStyleClass().contains("paused"));
    }

    @Test
    void withoutHabitsShowsTheEmptyStateWithAnAddButton() {
        open(AppData.empty());

        assertEquals(0, cards().size());
        assertNotNull(screen.lookup(".empty-state"));
        assertEquals("Dodaj nawyk", ((Button) screen.lookup(".primary-button")).getText());
    }

    @Test
    void aNewLogicalDayRebuildsTheScreen() {
        open(UiTestData.sample());

        clock.advance(Duration.ofHours(18)); // 04:00 w niedzielę
        FxTestSupport.onFx(model::refreshToday);

        assertEquals("Niedziela, 25 października 2026", ((Label) screen.lookup(".screen-subtitle")).getText());
        assertTrue(labelTexts(card("Spacer")).contains("Seria: 0 dni"), "wczorajszy brak przerywa serię");
    }
}
