package pl.mojrytm.service;

import java.time.LocalDate;
import java.util.List;
import pl.mojrytm.model.Entry;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.model.Ids;
import pl.mojrytm.model.Pause;
import pl.mojrytm.model.Schedule;

/**
 * Wspólne dane testów logiki. Kalendarz: "dziś" to sobota 2026-10-24, bieżący tydzień to
 * 19–25 października, poprzedni 12–18 października.
 */
final class Fixtures {

    static final LocalDate LONG_AGO = LocalDate.of(2026, 1, 1);

    // poprzedni tydzień
    static final LocalDate PREV_MON = LocalDate.of(2026, 10, 12);
    static final LocalDate PREV_SUN = LocalDate.of(2026, 10, 18);

    // bieżący tydzień
    static final LocalDate MON = LocalDate.of(2026, 10, 19);
    static final LocalDate TUE = MON.plusDays(1);
    static final LocalDate WED = MON.plusDays(2);
    static final LocalDate THU = MON.plusDays(3);
    static final LocalDate FRI = MON.plusDays(4);
    static final LocalDate SAT = MON.plusDays(5);
    static final LocalDate SUN = MON.plusDays(6);

    static final LocalDate TODAY = SAT;
    static final LocalDate NEXT_MON = MON.plusWeeks(1);

    private Fixtures() {
    }

    static Habit checkDaily(LocalDate created) {
        return new Habit("h1", null, "Spacer", HabitKind.CHECK, Schedule.DAILY, null, null, null, null, created, false);
    }

    static Habit checkWeekly(int timesPerWeek, LocalDate created) {
        return new Habit("h1", null, "Bieg", HabitKind.CHECK, Schedule.WEEKLY, timesPerWeek, null, null, null,
                created, false);
    }

    static Habit countDaily(double target, LocalDate created) {
        return new Habit("h1", null, "Woda", HabitKind.COUNT, Schedule.DAILY, null, target, "szklanki", null,
                created, false);
    }

    static Habit countWeekly(int timesPerWeek, double target, LocalDate created) {
        return new Habit("h1", null, "Pompki", HabitKind.COUNT, Schedule.WEEKLY, timesPerWeek, target, "powtórzenia",
                null, created, false);
    }

    /** Wpis zaliczenia nawyku CHECK. */
    static Entry done(LocalDate date) {
        return new Entry(Ids.newId(), "h1", date, 1.0, false);
    }

    /** Wpis zaliczenia wersją minimalną nawyku CHECK. */
    static Entry minimal(LocalDate date) {
        return new Entry(Ids.newId(), "h1", date, 1.0, true);
    }

    static Entry amount(LocalDate date, double value) {
        return new Entry(Ids.newId(), "h1", date, value, false);
    }

    static Entry minimalAmount(LocalDate date, double value) {
        return new Entry(Ids.newId(), "h1", date, value, true);
    }

    static Entry doneBy(String habitId, LocalDate date) {
        return new Entry(Ids.newId(), habitId, date, 1.0, false);
    }

    static Pause pause(LocalDate from, LocalDate to) {
        return new Pause(from, to, "choroba");
    }

    static Pauses noPauses() {
        return new Pauses(List.of());
    }

    static Pauses pauses(Pause... pauses) {
        return new Pauses(List.of(pauses));
    }

    static HabitTracker tracker(Habit habit, Entry... entries) {
        return new HabitTracker(habit, List.of(entries), noPauses());
    }

    static HabitTracker tracker(Habit habit, Pauses pauses, Entry... entries) {
        return new HabitTracker(habit, List.of(entries), pauses);
    }
}
