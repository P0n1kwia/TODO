package pl.mojrytm.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Entry;
import pl.mojrytm.model.Habit;

/**
 * Gotowe zestawienia dla widoków: UI niczego nie liczy, tylko rysuje te wyniki. Nawyki
 * zarchiwizowane są pomijane (ich historia zostaje w danych). Kolejność jak w danych.
 */
public final class HabitBoard {

    /** Nawyki aktywne dziś, czyli niezarchiwizowane i już założone. */
    public List<HabitRow> todayRows(AppData data, LocalDate today) {
        Pauses pauses = new Pauses(data.pauses());
        Map<String, List<Entry>> entries = entriesByHabit(data);
        return data.habits().stream()
                .filter(habit -> !habit.archived() && !habit.created().isAfter(today))
                .map(habit -> {
                    HabitTracker tracker = trackerOf(habit, entries, pauses);
                    return new HabitRow(habit, tracker.day(today, today), tracker.streak(today),
                            tracker.weekProgress(today, today));
                })
                .toList();
    }

    /** Nawyki widoczne w tygodniu zawierającym {@code anyDayOfWeek}: założone najpóźniej w jego niedzielę. */
    public List<WeekRow> weekRows(AppData data, LocalDate anyDayOfWeek, LocalDate today) {
        LocalDate weekEnd = Weeks.endOf(anyDayOfWeek);
        Pauses pauses = new Pauses(data.pauses());
        Map<String, List<Entry>> entries = entriesByHabit(data);
        return data.habits().stream()
                .filter(habit -> !habit.archived() && !habit.created().isAfter(weekEnd))
                .map(habit -> {
                    HabitTracker tracker = trackerOf(habit, entries, pauses);
                    return new WeekRow(habit, tracker.week(anyDayOfWeek, today), tracker.streak(today));
                })
                .toList();
    }

    public boolean isPaused(AppData data, LocalDate date) {
        return new Pauses(data.pauses()).isPaused(date);
    }

    private static Map<String, List<Entry>> entriesByHabit(AppData data) {
        return data.entries().stream().collect(Collectors.groupingBy(Entry::habitId));
    }

    private static HabitTracker trackerOf(Habit habit, Map<String, List<Entry>> entries, Pauses pauses) {
        return new HabitTracker(habit, entries.getOrDefault(habit.id(), List.of()), pauses);
    }
}
