package pl.mojrytm.ui;

import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalDouble;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.model.Schedule;

/**
 * Wartości z formularza nawyku, jeszcze jako tekst, wraz z walidacją i zamianą na {@link Habit}.
 * Osobna klasa, żeby dało się ją testować bez JavaFX.
 */
record HabitForm(
        String name,
        HabitKind kind,
        boolean weekly,
        int timesPerWeek,
        String targetText,
        String unit,
        String minimumVersion,
        LocalDate start) {

    enum Problem {
        NAME_EMPTY,
        TARGET_INVALID,
        START_MISSING
    }

    static HabitForm from(Habit habit) {
        return new HabitForm(habit.name(), habit.kind(), habit.schedule() == Schedule.WEEKLY,
                habit.timesPerWeek() == null ? 3 : habit.timesPerWeek(),
                habit.targetAmount() == null ? "" : Formats.amount(habit.targetAmount()),
                habit.unit() == null ? "" : habit.unit(),
                habit.minimumVersion() == null ? "" : habit.minimumVersion(), habit.created());
    }

    /** Pierwszy znaleziony problem albo pusty, gdy formularz jest poprawny. */
    Optional<Problem> problem() {
        if (name == null || name.isBlank()) {
            return Optional.of(Problem.NAME_EMPTY);
        }
        if (kind == HabitKind.COUNT) {
            OptionalDouble target = Formats.parseAmount(targetText);
            if (target.isEmpty() || target.getAsDouble() <= 0) {
                return Optional.of(Problem.TARGET_INVALID);
            }
        }
        if (start == null) {
            return Optional.of(Problem.START_MISSING);
        }
        return Optional.empty();
    }

    /** Buduje nawyk; formularz musi być poprawny ({@link #problem()} puste). */
    Habit toHabit(String id, String goalId, boolean archived) {
        if (problem().isPresent()) {
            throw new IllegalStateException("Formularz nawyku jest niepoprawny: " + problem().get());
        }
        boolean count = kind == HabitKind.COUNT;
        return new Habit(id, goalId, name.trim(), kind,
                weekly ? Schedule.WEEKLY : Schedule.DAILY,
                weekly ? timesPerWeek : null,
                count ? Formats.parseAmount(targetText).getAsDouble() : null,
                count ? blankToNull(unit) : null,
                blankToNull(minimumVersion), start, archived);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
