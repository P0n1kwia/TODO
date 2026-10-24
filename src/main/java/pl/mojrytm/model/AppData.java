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
    public static final int CURRENT_SCHEMA_VERSION = 1;

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

    /** Kopia z podmienioną listą wpisów; reszta danych bez zmian. */
    public AppData withEntries(List<Entry> newEntries) {
        return new AppData(schemaVersion, settings, areas, goals, habits, newEntries, tasks, notes, checkIns,
                counters, pauses, wheelRatings);
    }

    private static <T> List<T> copy(List<T> list) {
        return list == null ? List.of() : List.copyOf(list);
    }
}
