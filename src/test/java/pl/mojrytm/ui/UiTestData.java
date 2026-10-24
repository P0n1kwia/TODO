package pl.mojrytm.ui;

import java.time.LocalDate;
import java.util.List;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Entry;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.model.Schedule;

/** Dane do testów widoków. "Dziś" to sobota 2026-10-24 (zegar MutableClock). */
final class UiTestData {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 24);
    static final LocalDate YESTERDAY = TODAY.minusDays(1);
    static final LocalDate LONG_AGO = LocalDate.of(2026, 1, 1);

    private UiTestData() {
    }

    static Habit spacer() {
        return new Habit("spacer", null, "Spacer", HabitKind.CHECK, Schedule.DAILY, null, null, null,
                "Wyjść przed dom", LONG_AGO, false);
    }

    static Habit woda() {
        return new Habit("woda", null, "Woda", HabitKind.COUNT, Schedule.DAILY, null, 8.0, "szklanki", null,
                LONG_AGO, false);
    }

    static Habit bieg() {
        return new Habit("bieg", null, "Bieg", HabitKind.CHECK, Schedule.WEEKLY, 3, null, null, null, LONG_AGO, false);
    }

    static Habit stary() {
        return new Habit("stary", null, "Stary nawyk", HabitKind.CHECK, Schedule.DAILY, null, null, null, null,
                LONG_AGO, true);
    }

    /** Spacer zrobiony wczoraj, 3 szklanki wody dziś, jeden zarchiwizowany nawyk. */
    static AppData sample() {
        return AppData.empty()
                .withHabits(List.of(spacer(), woda(), bieg(), stary()))
                .withEntries(List.of(
                        new Entry("e1", "spacer", YESTERDAY, 1.0, false),
                        new Entry("e2", "woda", TODAY, 3.0, false)));
    }
}
