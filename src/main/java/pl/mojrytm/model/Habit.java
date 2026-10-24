package pl.mojrytm.model;

import java.time.LocalDate;

/**
 * Nawyk. Nie usuwamy, tylko archiwizujemy.
 *
 * @param goalId         cel, do którego należy nawyk (opcjonalnie)
 * @param timesPerWeek   1–7 dla {@link Schedule#WEEKLY}, null dla {@link Schedule#DAILY}
 * @param targetAmount   dzienny cel ilościowy, tylko dla {@link HabitKind#COUNT}
 * @param unit           jednostka (np. "szklanki"), tylko dla COUNT, opcjonalnie
 * @param minimumVersion opis wersji minimalnej nawyku (opcjonalnie)
 * @param created        dzień założenia nawyku; wcześniejsze dni nie liczą się nigdzie
 */
public record Habit(
        String id,
        String goalId,
        String name,
        HabitKind kind,
        Schedule schedule,
        Integer timesPerWeek,
        Double targetAmount,
        String unit,
        String minimumVersion,
        LocalDate created,
        boolean archived) {

    public Habit {
        Checks.requireText(id, "id");
        Checks.requireText(name, "name");
        Checks.requireNonNull(kind, "kind");
        Checks.requireNonNull(schedule, "schedule");
        Checks.requireNonNull(created, "created");

        if (schedule == Schedule.WEEKLY) {
            Checks.requireNonNull(timesPerWeek, "timesPerWeek");
            Checks.requireRange(timesPerWeek, 1, 7, "timesPerWeek");
        } else if (timesPerWeek != null) {
            throw new IllegalArgumentException("Pole 'timesPerWeek' dotyczy tylko nawyków tygodniowych");
        }

        if (kind == HabitKind.COUNT) {
            Checks.requireNonNull(targetAmount, "targetAmount");
            if (!(targetAmount > 0) || Double.isInfinite(targetAmount)) {
                throw new IllegalArgumentException("Pole 'targetAmount' musi być dodatnie, jest: " + targetAmount);
            }
        } else if (targetAmount != null) {
            throw new IllegalArgumentException("Pole 'targetAmount' dotyczy tylko nawyków typu COUNT");
        }
    }
}
