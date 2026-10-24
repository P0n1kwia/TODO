package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.mojrytm.service.Fixtures.FRI;
import static pl.mojrytm.service.Fixtures.LONG_AGO;
import static pl.mojrytm.service.Fixtures.MON;
import static pl.mojrytm.service.Fixtures.SAT;
import static pl.mojrytm.service.Fixtures.THU;
import static pl.mojrytm.service.Fixtures.TODAY;
import static pl.mojrytm.service.Fixtures.TUE;
import static pl.mojrytm.service.Fixtures.WED;
import static pl.mojrytm.service.Fixtures.pause;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Entry;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.model.Pause;
import pl.mojrytm.model.Schedule;

/** Dziś to sobota, więc dzienny nawyk ma w tym tygodniu 6 oczekiwanych dni (pon–sob). */
class WeekSummaryTest {

    private final HabitBoard board = new HabitBoard();

    private static Habit habit(String id, boolean archived) {
        return new Habit(id, null, "Nawyk " + id, HabitKind.CHECK, Schedule.DAILY, null, null, null, null,
                LONG_AGO, archived);
    }

    private static List<Entry> days(String habitId, LocalDate... dates) {
        List<Entry> entries = new ArrayList<>();
        for (LocalDate date : dates) {
            entries.add(Fixtures.doneBy(habitId, date));
        }
        return entries;
    }

    private static AppData data(List<Habit> habits, List<Entry> entries, List<Pause> pauses) {
        return AppData.empty().withHabits(habits).withEntries(entries).withPauses(pauses);
    }

    private static String idOf(Optional<WeekRow> row) {
        return row.orElseThrow().habit().id();
    }

    @Test
    void bestAndWeakestAreTheHighestAndLowestPercent() {
        List<Entry> entries = new ArrayList<>(days("a", MON, TUE, WED, THU, FRI, SAT));
        entries.addAll(days("b", MON, TUE, WED));

        WeekSummary summary = board.weekSummary(
                data(List.of(habit("b", false), habit("c", false), habit("a", false)), entries, List.of()), TODAY, TODAY);

        assertEquals(List.of("b", "c", "a"), summary.rows().stream().map(row -> row.habit().id()).toList());
        assertEquals("a", idOf(summary.best()));
        assertEquals("c", idOf(summary.weakest()));
    }

    @Test
    void tiesPickTheFirstInDataOrder() {
        List<Entry> entries = new ArrayList<>(days("a", MON, TUE, WED));
        entries.addAll(days("b", MON, TUE, WED));
        entries.addAll(days("c", MON));
        entries.addAll(days("d", MON));

        WeekSummary summary = board.weekSummary(data(List.of(habit("a", false), habit("b", false),
                habit("c", false), habit("d", false)), entries, List.of()), TODAY, TODAY);

        assertEquals("a", idOf(summary.best()));
        assertEquals("c", idOf(summary.weakest()));
    }

    @Test
    void whenEveryoneHasTheSameResultThereIsABestButNoWeakest() {
        List<Entry> entries = new ArrayList<>(days("a", MON, TUE));
        entries.addAll(days("b", MON, TUE));

        WeekSummary summary = board.weekSummary(
                data(List.of(habit("a", false), habit("b", false)), entries, List.of()), TODAY, TODAY);

        assertEquals("a", idOf(summary.best()));
        assertEquals(Optional.empty(), summary.weakest());
    }

    @Test
    void aSingleHabitIsTheBestAndNeverTheWeakest() {
        WeekSummary summary = board.weekSummary(data(List.of(habit("a", false)), List.of(), List.of()), TODAY, TODAY);

        assertEquals("a", idOf(summary.best()));
        assertEquals(Optional.empty(), summary.weakest());
    }

    @Test
    void habitsWithNothingExpectedDoNotTakePartInTheComparison() {
        List<Entry> entries = new ArrayList<>(days("a", MON));

        WeekSummary summary = board.weekSummary(data(List.of(habit("a", false), habit("paused", false)), entries,
                List.of(pause(MON, MON.plusDays(6)))), TODAY, TODAY);

        // pauza obejmuje cały tydzień, więc żaden nawyk nie ma oczekiwań
        assertEquals(2, summary.rows().size());
        assertEquals(Optional.empty(), summary.best());
        assertEquals(Optional.empty(), summary.weakest());
    }

    @Test
    void archivedHabitsAreLeftOut() {
        WeekSummary summary = board.weekSummary(
                data(List.of(habit("a", false), habit("old", true)), List.of(), List.of()), TODAY, TODAY);

        assertEquals(List.of("a"), summary.rows().stream().map(row -> row.habit().id()).toList());
    }

    @Test
    void noHabitsGiveAnEmptySummary() {
        WeekSummary summary = board.weekSummary(AppData.empty(), TODAY, TODAY);

        assertEquals(List.of(), summary.rows());
        assertTrue(summary.best().isEmpty());
        assertTrue(summary.weakest().isEmpty());
    }

    @Test
    void aPastWeekIsSummarizedByItsOwnDays() {
        LocalDate previousMonday = MON.minusWeeks(1);
        List<Entry> entries = days("a", previousMonday, previousMonday.plusDays(1));

        WeekSummary summary = board.weekSummary(data(List.of(habit("a", false)), entries, List.of()), previousMonday,
                TODAY);

        assertEquals(new WeekProgress(2, 7), summary.rows().get(0).week().progress());
    }
}
