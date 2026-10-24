package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pl.mojrytm.service.Fixtures.FRI;
import static pl.mojrytm.service.Fixtures.LONG_AGO;
import static pl.mojrytm.service.Fixtures.SAT;
import static pl.mojrytm.service.Fixtures.SUN;
import static pl.mojrytm.service.Fixtures.THU;
import static pl.mojrytm.service.Fixtures.TODAY;
import static pl.mojrytm.service.Fixtures.WED;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Entry;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.model.Schedule;
import pl.mojrytm.model.Settings;
import pl.mojrytm.service.EntryRejectedException.Reason;

class EntryEditorTest {

    private static final String CHECK_ID = "check";
    private static final String COUNT_ID = "count";

    private final AtomicInteger idCounter = new AtomicInteger();
    private final EntryEditor editor = editorAt("2026-10-24T10:00:00Z");

    private EntryEditor editorAt(String instant) {
        LogicalClock clock = new LogicalClock(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
        return new EntryEditor(clock, () -> "e" + idCounter.incrementAndGet());
    }

    private static Habit checkHabit(LocalDate created, boolean archived) {
        return new Habit(CHECK_ID, null, "Spacer", HabitKind.CHECK, Schedule.DAILY, null, null, null, null,
                created, archived);
    }

    private static Habit countHabit() {
        return new Habit(COUNT_ID, null, "Woda", HabitKind.COUNT, Schedule.DAILY, null, 8.0, "szklanki", null,
                LONG_AGO, false);
    }

    private static AppData dataWith(int dayStartHour, List<Entry> entries, Habit... habits) {
        return new AppData(1, new Settings(dayStartHour, null), null, null, List.of(habits), entries,
                null, null, null, null, null, null);
    }

    private static AppData dataWith(Habit... habits) {
        return dataWith(4, List.of(), habits);
    }

    private static AppData dataWith(List<Entry> entries) {
        return dataWith(4, entries, checkHabit(LONG_AGO, false), countHabit());
    }

    private static AppData empty() {
        return dataWith(List.of());
    }

    private static Entry check(String id, LocalDate date, boolean minimal) {
        return new Entry(id, CHECK_ID, date, 1.0, minimal);
    }

    private static Entry count(String id, LocalDate date, double value, boolean minimal) {
        return new Entry(id, COUNT_ID, date, value, minimal);
    }

    private static Reason reasonOf(Runnable action) {
        return assertThrows(EntryRejectedException.class, action::run).reason();
    }

    @Test
    void checkingCreatesOneEntryForTheDay() {
        AppData result = editor.setChecked(empty(), CHECK_ID, FRI, true, false);

        assertEquals(List.of(check("e1", FRI, false)), result.entries());
    }

    @Test
    void checkingTwiceKeepsASingleEntryWithTheSameId() {
        AppData once = editor.setChecked(empty(), CHECK_ID, FRI, true, false);

        AppData twice = editor.setChecked(once, CHECK_ID, FRI, true, false);

        assertSame(once, twice);
        assertEquals(1, twice.entries().size());
    }

    @Test
    void checkingWithMinimalVersionSetsTheFlagAndCanBeUpgraded() {
        AppData minimal = editor.setChecked(empty(), CHECK_ID, FRI, true, true);
        AppData full = editor.setChecked(minimal, CHECK_ID, FRI, true, false);

        assertEquals(List.of(check("e1", FRI, true)), minimal.entries());
        assertEquals(List.of(check("e1", FRI, false)), full.entries());
    }

    @Test
    void uncheckingRemovesTheEntryAndUncheckingNothingChangesNothing() {
        AppData checked = editor.setChecked(empty(), CHECK_ID, FRI, true, false);

        AppData unchecked = editor.setChecked(checked, CHECK_ID, FRI, false, false);

        assertEquals(List.of(), unchecked.entries());
        assertSame(unchecked, editor.setChecked(unchecked, CHECK_ID, FRI, false, false));
    }

    @Test
    void uncheckingRemovesDuplicatesLeftByEditedFiles() {
        AppData data = dataWith(List.of(check("a", FRI, false), check("b", FRI, true), check("c", THU, false)));

        AppData result = editor.setChecked(data, CHECK_ID, FRI, false, false);

        assertEquals(List.of(check("c", THU, false)), result.entries());
    }

    @Test
    void otherDaysAndOtherHabitsAreUntouched() {
        Entry otherDay = check("x", THU, false);
        Entry otherHabit = count("y", FRI, 3, false);
        AppData data = dataWith(List.of(otherDay, otherHabit));

        AppData result = editor.setChecked(data, CHECK_ID, FRI, true, false);

        assertEquals(List.of(otherDay, otherHabit, check("e1", FRI, false)), result.entries());
    }

    @Test
    void theOriginalDataIsNeverModified() {
        AppData data = empty();

        editor.setChecked(data, CHECK_ID, FRI, true, false);

        assertEquals(List.of(), data.entries());
    }

    @Test
    void todayYesterdayAndTheCreationDayAreAllowed() {
        AppData data = dataWith(checkHabit(WED, false));

        assertEquals(1, editor.setChecked(data, CHECK_ID, TODAY, true, false).entries().size());
        assertEquals(1, editor.setChecked(data, CHECK_ID, TODAY.minusDays(1), true, false).entries().size());
        assertEquals(1, editor.setChecked(data, CHECK_ID, WED, true, false).entries().size());
    }

    @Test
    void aDayInTheFutureIsRejected() {
        assertEquals(Reason.FUTURE_DAY, reasonOf(() -> editor.setChecked(empty(), CHECK_ID, SUN, true, false)));
    }

    @Test
    void aDayBeforeTheHabitWasCreatedIsRejected() {
        AppData data = dataWith(checkHabit(WED, false));

        assertEquals(Reason.BEFORE_HABIT_CREATED,
                reasonOf(() -> editor.setChecked(data, CHECK_ID, WED.minusDays(1), true, false)));
    }

    @Test
    void theLogicalDayDecidesWhatIsTheFuture() {
        // 01:30 w niedzielę 25.10 to jeszcze sobota 24.10 (dzień zaczyna się o 4:00)
        EntryEditor night = editorAt("2026-10-25T01:30:00Z");
        AppData startsAtFour = dataWith(4, List.of(), checkHabit(LONG_AGO, false));
        AppData startsAtMidnight = dataWith(0, List.of(), checkHabit(LONG_AGO, false));

        assertEquals(1, night.setChecked(startsAtFour, CHECK_ID, SAT, true, false).entries().size());
        assertEquals(Reason.FUTURE_DAY, reasonOf(() -> night.setChecked(startsAtFour, CHECK_ID, SUN, true, false)));
        assertEquals(1, night.setChecked(startsAtMidnight, CHECK_ID, SUN, true, false).entries().size());
    }

    @Test
    void archivedHabitCannotBeEdited() {
        AppData data = dataWith(checkHabit(LONG_AGO, true));

        assertEquals(Reason.HABIT_ARCHIVED, reasonOf(() -> editor.setChecked(data, CHECK_ID, FRI, true, false)));
    }

    @Test
    void unknownHabitIsRejected() {
        assertEquals(Reason.HABIT_NOT_FOUND, reasonOf(() -> editor.setChecked(empty(), "nie ma", FRI, true, false)));
    }

    @Test
    void checkingACountHabitAndCountingACheckHabitAreRejected() {
        assertEquals(Reason.WRONG_KIND, reasonOf(() -> editor.setChecked(empty(), COUNT_ID, FRI, true, false)));
        assertEquals(Reason.WRONG_KIND, reasonOf(() -> editor.setAmount(empty(), CHECK_ID, FRI, 2, false)));
        assertEquals(Reason.WRONG_KIND, reasonOf(() -> editor.addAmount(empty(), CHECK_ID, FRI, 1)));
    }

    @Test
    void settingAnAmountCreatesOneEntry() {
        AppData result = editor.setAmount(empty(), COUNT_ID, FRI, 5.5, false);

        assertEquals(List.of(count("e1", FRI, 5.5, false)), result.entries());
    }

    @Test
    void settingAnAmountReplacesAllEntriesOfTheDayInPlace() {
        Entry before = count("a", THU, 1, false);
        Entry after = count("d", SAT, 4, false);
        AppData data = dataWith(List.of(before, count("b", FRI, 2, false), count("c", FRI, 3, true), after));

        AppData result = editor.setAmount(data, COUNT_ID, FRI, 8, false);

        assertEquals(List.of(before, count("b", FRI, 8, false), after), result.entries());
    }

    @Test
    void zeroWithoutMinimalVersionRemovesTheEntry() {
        AppData data = dataWith(List.of(count("a", FRI, 2, false)));

        assertEquals(List.of(), editor.setAmount(data, COUNT_ID, FRI, 0, false).entries());
    }

    @Test
    void zeroWithMinimalVersionKeepsAMinimalEntry() {
        AppData result = editor.setAmount(empty(), COUNT_ID, FRI, 0, true);

        assertEquals(List.of(count("e1", FRI, 0, true)), result.entries());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, -0.1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void invalidAmountsAreRejected(double amount) {
        assertEquals(Reason.INVALID_AMOUNT, reasonOf(() -> editor.setAmount(empty(), COUNT_ID, FRI, amount, false)));
    }

    @Test
    void addingAmountsAccumulatesIntoTheDaySum() {
        AppData one = editor.addAmount(empty(), COUNT_ID, FRI, 1);
        AppData three = editor.addAmount(editor.addAmount(one, COUNT_ID, FRI, 1), COUNT_ID, FRI, 1);

        assertEquals(List.of(count("e1", FRI, 3, false)), three.entries());
    }

    @Test
    void addingDecimalsDoesNotLeakFloatingPointNoiseIntoTheFile() {
        AppData result = editor.addAmount(editor.addAmount(empty(), COUNT_ID, FRI, 0.1), COUNT_ID, FRI, 0.2);

        assertEquals(0.3, result.entries().get(0).value());
    }

    @Test
    void subtractingNeverGoesBelowZeroAndEmptiesTheDay() {
        AppData data = dataWith(List.of(count("a", FRI, 2, false)));

        assertEquals(List.of(count("a", FRI, 1, false)), editor.addAmount(data, COUNT_ID, FRI, -1).entries());
        assertEquals(List.of(), editor.addAmount(data, COUNT_ID, FRI, -5).entries());
    }

    @Test
    void addingKeepsTheMinimalFlagOfTheDay() {
        AppData data = dataWith(List.of(count("a", FRI, 2, true)));

        assertEquals(List.of(count("a", FRI, 3, true)), editor.addAmount(data, COUNT_ID, FRI, 1).entries());
    }

    @Test
    void addingToAFutureDayIsRejectedAndNonFiniteDeltaToo() {
        assertEquals(Reason.FUTURE_DAY, reasonOf(() -> editor.addAmount(empty(), COUNT_ID, SUN, 1)));
        assertEquals(Reason.INVALID_AMOUNT, reasonOf(() -> editor.addAmount(empty(), COUNT_ID, FRI, Double.NaN)));
    }

    @Test
    void editedEntriesAreVisibleToTheTracker() {
        AppData data = editor.setChecked(empty(), CHECK_ID, TODAY, true, true);
        Habit habit = data.habits().get(0);

        HabitTracker tracker = new HabitTracker(habit, data.entries(), new Pauses(data.pauses()));

        assertEquals(DayStatus.DONE_MINIMAL, tracker.day(TODAY, TODAY).status());
        assertEquals(new Streak(1, StreakUnit.DAYS), tracker.streak(TODAY));
    }
}
