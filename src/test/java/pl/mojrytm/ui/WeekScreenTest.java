package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.mojrytm.model.Entry;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.model.Pause;
import pl.mojrytm.model.Schedule;
import pl.mojrytm.storage.JsonStorage;

class WeekScreenTest {

    private static final LocalDate MON = LocalDate.of(2026, 10, 19);
    private static final LocalDate WED = MON.plusDays(2);
    private static final LocalDate SAT = MON.plusDays(5);
    private static final LocalDate SUN = MON.plusDays(6);
    private static final LocalDate PREV_MON = MON.minusWeeks(1);

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock("2026-10-24T10:00:00Z");
    private AppModel model;
    private WeekScreen screen;

    @BeforeAll
    static void toolkit() {
        FxTestSupport.assumeToolkit();
    }

    @BeforeEach
    void setUp() {
        Habit nowy = new Habit("nowy", null, "Nowy nawyk", HabitKind.CHECK, Schedule.DAILY, null, null, null, null,
                WED, false);
        model = new AppModel(UiTestData.sample().withHabits(
                List.of(UiTestData.spacer(), UiTestData.woda(), UiTestData.bieg(), UiTestData.stary(), nowy)),
                new JsonStorage(dir, clock), clock);
        screen = FxTestSupport.onFx(() -> new WeekScreen(model));
    }

    @AfterEach
    void tearDown() throws IOException {
        model.shutdown();
    }

    private Button cell(String habitId, LocalDate date) {
        return (Button) screen.lookup("#cell-" + habitId + "-" + date);
    }

    private void click(String habitId, LocalDate date) {
        Button button = cell(habitId, date);
        assertNotNull(button, "brak kratki " + habitId + " " + date);
        FxTestSupport.onFx(button::fire);
    }

    private void press(String styleClass) {
        FxTestSupport.onFx(((Button) screen.lookup("." + styleClass))::fire);
    }

    private List<Entry> entriesOf(String habitId) {
        return model.data().entries().stream().filter(entry -> entry.habitId().equals(habitId)).toList();
    }

    private String rangeText() {
        return ((Label) screen.lookup(".week-range")).getText();
    }

    @Test
    void showsTheWeekRangeTheDayHeadersAndOnlyActiveHabits() {
        assertEquals("19–25 października 2026", rangeText());
        List<String> headers = screen.lookupAll(".day-header").stream().map(n -> ((Label) n).getText()).toList();
        assertEquals(List.of("Pn\n19", "Wt\n20", "Śr\n21", "Cz\n22", "Pt\n23", "Sb\n24", "Nd\n25", "Wykonanie", "Seria dziś"),
                headers);
        assertNull(cell("stary", MON));
        assertNotNull(cell("spacer", MON));
        assertEquals(4 * 7, screen.lookupAll(".day-cell").size());
    }

    @Test
    void highlightsTodayInTheHeader() {
        List<String> today = screen.lookupAll(".day-header.today").stream().map(n -> ((Label) n).getText()).toList();

        assertEquals(List.of("Sb\n24"), today);
    }

    @Test
    void cellsShowSymbolsAndAmounts() {
        assertEquals("✓", cell("spacer", UiTestData.YESTERDAY).getText());
        assertEquals("", cell("spacer", MON).getText());
        assertEquals("3", cell("woda", UiTestData.TODAY).getText());
        assertTrue(cell("spacer", UiTestData.YESTERDAY).getStyleClass().contains("done"));
    }

    @Test
    void showsWeekProgressAndStreakInTheRow() {
        List<String> progress = screen.lookupAll(".week-progress").stream().map(n -> ((Label) n).getText()).toList();
        List<String> streaks = screen.lookupAll(".streak-label").stream().map(n -> ((Label) n).getText()).toList();

        assertEquals("1/6 (17%)", progress.get(0));
        assertEquals("Seria: 1 dzień", streaks.get(0));
    }

    @Test
    void clickingAnEmptyPastCellCyclesThroughDoneMinimalAndEmptyForAHabitWithAMinimumVersion() {
        click("spacer", MON);
        assertEquals(List.of(false), minimalFlagsOn("spacer", MON));
        assertEquals("✓", cell("spacer", MON).getText());

        click("spacer", MON);
        assertEquals(List.of(true), minimalFlagsOn("spacer", MON));
        assertEquals("(✓)", cell("spacer", MON).getText());
        assertTrue(cell("spacer", MON).getStyleClass().contains("minimal"));

        click("spacer", MON);
        assertEquals(List.of(), minimalFlagsOn("spacer", MON));
        assertEquals("", cell("spacer", MON).getText());
    }

    @Test
    void aHabitWithoutAMinimumVersionTogglesBetweenDoneAndEmpty() {
        click("bieg", MON);
        assertEquals(List.of(false), minimalFlagsOn("bieg", MON));

        click("bieg", MON);
        assertEquals(List.of(), minimalFlagsOn("bieg", MON));
    }

    private List<Boolean> minimalFlagsOn(String habitId, LocalDate date) {
        return entriesOf(habitId).stream().filter(e -> e.date().equals(date)).map(Entry::minimal).toList();
    }

    @Test
    void futureDaysAndDaysBeforeCreationAreLocked() {
        assertTrue(cell("spacer", SUN).isDisabled());
        assertTrue(cell("spacer", SUN).getStyleClass().contains("unavailable"));
        assertTrue(cell("nowy", MON).isDisabled());
        assertFalse(cell("nowy", WED).isDisabled());
        assertFalse(cell("spacer", SAT).isDisabled());
    }

    @Test
    void pausedDaysAreMarkedButStillEditable() {
        FxTestSupport.onFx(() -> model.update(data -> model.pauses().add(data, new Pause(MON, MON.plusDays(1), "choroba"))));

        assertTrue(cell("spacer", MON).getStyleClass().contains("paused"));
        assertFalse(cell("spacer", MON).isDisabled());
        assertFalse(cell("spacer", WED).getStyleClass().contains("paused"));
    }

    @Test
    void navigatesToThePreviousWeekAndBackWithoutGoingIntoTheFuture() {
        assertTrue(screen.lookup(".next-week").isDisabled());
        assertTrue(screen.lookup(".current-week").isDisabled());

        press("previous-week");
        assertEquals("12–18 października 2026", rangeText());
        assertFalse(screen.lookup(".next-week").isDisabled());
        assertNotNull(cell("spacer", PREV_MON));
        assertNull(cell("nowy", PREV_MON), "nawyk założony po tym tygodniu nie jest tu widoczny");

        press("next-week");
        assertEquals("19–25 października 2026", rangeText());
        assertTrue(screen.lookup(".next-week").isDisabled());

        press("previous-week");
        press("previous-week");
        assertEquals("5–11 października 2026", rangeText());
        press("current-week");
        assertEquals("19–25 października 2026", rangeText());
    }

    @Test
    void pastWeeksCanBeBackfilled() {
        press("previous-week");

        click("spacer", PREV_MON);

        assertEquals(List.of(false), minimalFlagsOn("spacer", PREV_MON));
        assertEquals("12–18 października 2026", rangeText());
    }

    @Test
    void aNewLogicalDayKeepsTheViewedWeekRelativeToToday() {
        press("previous-week");

        clock.advance(Duration.ofDays(2));
        FxTestSupport.onFx(model::refreshToday);

        assertEquals("19–25 października 2026", rangeText());
    }

    @Test
    void countCellsAreClickable() {
        assertFalse(cell("woda", MON).isDisabled());
    }
}
