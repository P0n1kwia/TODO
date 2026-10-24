package pl.mojrytm.model;

import java.util.List;

/**
 * Całość danych aplikacji, czyli zawartość pliku JSON. Niezmienna migawka:
 * zmiana oznacza utworzenie nowej instancji, dzięki czemu zapis w tle jest bezpieczny.
 * Zapisujemy tylko fakty, wartości wyliczane (serie, procenty) nigdy tu nie trafiają.
 */
public record AppData(
        int schemaVersion,
        Settings settings,
        List<Area> areas,
        List<Goal> goals,
        List<Habit> habits,
        List<Entry> entries,
        List<Task> tasks,
        List<Note> notes,
        List<CheckIn> checkIns,
        List<Counter> counters,
        List<Pause> pauses,
        List<WheelRating> wheelRatings) {

    /** Bieżąca wersja schematu. Zmiana modelu = podbicie + migracja w storage.Migrator + test. */
    public static final int CURRENT_SCHEMA_VERSION = 2;

    public AppData {
        settings = settings == null ? Settings.defaults() : settings;
        areas = copy(areas);
        goals = copy(goals);
        habits = copy(habits);
        entries = copy(entries);
        tasks = copy(tasks);
        notes = copy(notes);
        checkIns = copy(checkIns);
        counters = copy(counters);
        pauses = copy(pauses);
        wheelRatings = copy(wheelRatings);
    }

    public static AppData empty() {
        return new AppData(CURRENT_SCHEMA_VERSION, Settings.defaults(),
                null, null, null, null, null, null, null, null, null, null);
    }

    /** Kopia z podmienioną listą obszarów; reszta danych bez zmian. */
    public AppData withAreas(List<Area> newAreas) {
        return new AppData(schemaVersion, settings, newAreas, goals, habits, entries, tasks, notes, checkIns,
                counters, pauses, wheelRatings);
    }

    /** Kopia z podmienioną listą celów; reszta danych bez zmian. */
    public AppData withGoals(List<Goal> newGoals) {
        return new AppData(schemaVersion, settings, areas, newGoals, habits, entries, tasks, notes, checkIns,
                counters, pauses, wheelRatings);
    }

    /** Kopia z podmienioną listą nawyków; reszta danych bez zmian. */
    public AppData withHabits(List<Habit> newHabits) {
        return new AppData(schemaVersion, settings, areas, goals, newHabits, entries, tasks, notes, checkIns,
                counters, pauses, wheelRatings);
    }

    /** Kopia z podmienioną listą ocen koła życia; reszta danych bez zmian. */
    public AppData withWheelRatings(List<WheelRating> newWheelRatings) {
        return new AppData(schemaVersion, settings, areas, goals, habits, entries, tasks, notes, checkIns,
                counters, pauses, newWheelRatings);
    }

    /** Kopia z podmienioną listą notatek; reszta danych bez zmian. */
    public AppData withNotes(List<Note> newNotes) {
        return new AppData(schemaVersion, settings, areas, goals, habits, entries, tasks, newNotes, checkIns,
                counters, pauses, wheelRatings);
    }

    /** Kopia z podmienioną listą liczników; reszta danych bez zmian. */
    public AppData withCounters(List<Counter> newCounters) {
        return new AppData(schemaVersion, settings, areas, goals, habits, entries, tasks, notes, checkIns,
                newCounters, pauses, wheelRatings);
    }

    /** Kopia z podmienioną listą pauz; reszta danych bez zmian. */
    public AppData withPauses(List<Pause> newPauses) {
        return new AppData(schemaVersion, settings, areas, goals, habits, entries, tasks, notes, checkIns,
                counters, newPauses, wheelRatings);
    }

    /** Kopia z podmienioną listą wpisów; reszta danych bez zmian. */
    public AppData withEntries(List<Entry> newEntries) {
        return new AppData(schemaVersion, settings, areas, goals, habits, newEntries, tasks, notes, checkIns,
                counters, pauses, wheelRatings);
    }

    private static <T> List<T> copy(List<T> list) {
        return list == null ? List.of() : List.copyOf(list);
    }
}
