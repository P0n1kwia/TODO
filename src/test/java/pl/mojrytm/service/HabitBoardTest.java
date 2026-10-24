package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.mojrytm.service.Fixtures.FRI;
import static pl.mojrytm.service.Fixtures.LONG_AGO;
import static pl.mojrytm.service.Fixtures.MON;
import static pl.mojrytm.service.Fixtures.PREV_MON;
import static pl.mojrytm.service.Fixtures.SAT;
import static pl.mojrytm.service.Fixtures.SUN;
import static pl.mojrytm.service.Fixtures.TODAY;
import static pl.mojrytm.service.Fixtures.pause;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Entry;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.model.Pause;
import pl.mojrytm.model.Schedule;

class HabitBoardTest {

    private final HabitBoard board = new HabitBoard();

    private static Habit habit(String id, LocalDate created, boolean archived) {
        return new Habit(id, null, "Nawyk " + id, HabitKind.CHECK, Schedule.DAILY, null, null, null, null,
                created, archived);
    }

    private static Entry done(String habitId, LocalDate date) {
        return Fixtures.doneBy(habitId, date);
    }

    private static AppData data(List<Habit> habits, List<Entry> entries, List<Pause> pauses) {
        return new AppData(AppData.CURRENT_SCHEMA_VERSION, null, null, null, habits, entries, null, null, null, null, pauses, null);
    }

    @Test
    void todayRowsHaveTheTodayCellStreakAndWeekProgress() {
        AppData data = data(List.of(habit("a", LONG_AGO, false)),
                List.of(done("a", TODAY), done("a", FRI)), List.of());

        HabitRow row = board.todayRows(data, TODAY).get(0);

        assertEquals("a", row.habit().id());
        assertEquals(DayStatus.DONE, row.today().status());
        assertEquals(TODAY, row.today().date());
        assertEquals(new Streak(2, StreakUnit.DAYS), row.streak());
        assertEquals(new WeekProgress(2, 6), row.week());
    }

    @Test
    void todayRowsSkipArchivedAndNotYetStartedHabitsAndKeepDataOrder() {
        AppData data = data(List.of(
                habit("b", LONG_AGO, false),
                habit("archived", LONG_AGO, true),
                habit("future", SUN, false),
                habit("startsToday", TODAY, false),
                habit("a", LONG_AGO, false)), List.of(), List.of());

        List<String> ids = board.todayRows(data, TODAY).stream().map(row -> row.habit().id()).toList();

        assertEquals(List.of("b", "startsToday", "a"), ids);
    }

    @Test
    void entriesOfOneHabitDoNotLeakIntoAnother() {
        AppData data = data(List.of(habit("a", LONG_AGO, false), habit("b", LONG_AGO, false)),
                List.of(done("a", TODAY)), List.of());

        List<HabitRow> rows = board.todayRows(data, TODAY);

        assertEquals(DayStatus.DONE, rows.get(0).today().status());
        assertEquals(DayStatus.NOT_DONE, rows.get(1).today().status());
    }

    @Test
    void pausesAreVisibleInTheRows() {
        AppData data = data(List.of(habit("a", LONG_AGO, false)), List.of(), List.of(pause(TODAY, TODAY)));

        assertTrue(board.todayRows(data, TODAY).get(0).today().paused());
        assertTrue(board.isPaused(data, TODAY));
        assertFalse(board.isPaused(data, FRI));
    }

    @Test
    void weekRowsShowTheSevenDaysOfTheRequestedWeek() {
        AppData data = data(List.of(habit("a", LONG_AGO, false)), List.of(done("a", MON)), List.of());

        WeekRow row = board.weekRows(data, SAT, TODAY).get(0);

        assertEquals(MON, row.week().weekStart());
        assertEquals(7, row.week().days().size());
        assertEquals(DayStatus.DONE, row.week().days().get(0).status());
        assertEquals(new Streak(0, StreakUnit.DAYS), row.streak());
    }

    @Test
    void weekRowsListHabitsStartedByTheEndOfThatWeekOnly() {
        AppData data = data(List.of(
                habit("old", LONG_AGO, false),
                habit("startsSunday", SUN, false),
                habit("nextWeek", SUN.plusDays(1), false),
                habit("archived", LONG_AGO, true)), List.of(), List.of());

        List<String> thisWeek = board.weekRows(data, TODAY, TODAY).stream().map(r -> r.habit().id()).toList();
        List<String> lastWeek = board.weekRows(data, PREV_MON, TODAY).stream().map(r -> r.habit().id()).toList();

        assertEquals(List.of("old", "startsSunday"), thisWeek);
        assertEquals(List.of("old"), lastWeek);
    }

    @Test
    void noHabitsGiveNoRows() {
        assertEquals(List.of(), board.todayRows(AppData.empty(), TODAY));
        assertEquals(List.of(), board.weekRows(AppData.empty(), TODAY, TODAY));
    }
}
