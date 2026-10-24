package pl.mojrytm.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import pl.mojrytm.model.Entry;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.model.Schedule;

/**
 * Wyniki jednego nawyku: stan dni, wykonanie tygodnia i serie. Czysta logika bez UI i zegara:
 * "dzisiaj" (dzień logiczny) podaje wołający.
 *
 * <p>Reguły dni nieliczonych: dni przed założeniem nie liczą się nigdzie, dni w pauzie nie
 * wchodzą do mianownika procentów i nie przerywają serii.
 */
public final class HabitTracker {

    /** Tolerancja przy porównywaniu sum z celem (wartości to double). */
    private static final double EPSILON = 1e-9;

    private final Habit habit;
    private final Pauses pauses;
    private final Map<LocalDate, List<Entry>> entriesByDate;

    /** @param entries dowolne wpisy; brane są tylko te tego nawyku */
    public HabitTracker(Habit habit, Collection<Entry> entries, Pauses pauses) {
        this.habit = habit;
        this.pauses = pauses;
        this.entriesByDate = entries.stream()
                .filter(entry -> entry.habitId().equals(habit.id()))
                .collect(Collectors.groupingBy(Entry::date));
    }

    public DayCell day(LocalDate date, LocalDate today) {
        return new DayCell(date, statusOn(date), amountOn(date), pauses.isPaused(date),
                date.isBefore(habit.created()), date.isAfter(today));
    }

    /** Tydzień (poniedziałek–niedziela) zawierający {@code anyDayOfWeek}. */
    public WeekView week(LocalDate anyDayOfWeek, LocalDate today) {
        LocalDate start = Weeks.startOf(anyDayOfWeek);
        List<DayCell> cells = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            cells.add(day(start.plusDays(i), today));
        }
        return new WeekView(start, List.copyOf(cells), weekProgress(start, today));
    }

    /**
     * Wykonanie tygodnia zawierającego {@code anyDayOfWeek}.
     *
     * <p>Nawyk dzienny: oczekiwane to dni od założenia do dziś (w przeszłym tygodniu do niedzieli),
     * bez dni pauzy; liczą się tylko zaliczone dni poza pauzą.
     * Nawyk tygodniowy: oczekiwane to {@code min(N, dni dostępne)}, gdzie dostępne to dni od założenia
     * poza pauzą (także te jeszcze przed nami); liczą się wszystkie dni z zaliczeniem, także w pauzie.
     */
    public WeekProgress weekProgress(LocalDate anyDayOfWeek, LocalDate today) {
        LocalDate start = Weeks.startOf(anyDayOfWeek);
        LocalDate end = start.plusDays(6);
        return habit.schedule() == Schedule.DAILY
                ? dailyProgress(start, end, today)
                : weeklyProgress(start, end, today);
    }

    private WeekProgress dailyProgress(LocalDate start, LocalDate end, LocalDate today) {
        int done = 0;
        int expected = 0;
        LocalDate last = end.isBefore(today) ? end : today;
        for (LocalDate date = start; !date.isAfter(last); date = date.plusDays(1)) {
            if (date.isBefore(habit.created()) || pauses.isPaused(date)) {
                continue;
            }
            expected++;
            if (statusOn(date).isDone()) {
                done++;
            }
        }
        return new WeekProgress(done, expected);
    }

    private WeekProgress weeklyProgress(LocalDate start, LocalDate end, LocalDate today) {
        int available = 0;
        int done = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (date.isBefore(habit.created())) {
                continue;
            }
            if (!pauses.isPaused(date)) {
                available++;
            }
            if (!date.isAfter(today) && statusOn(date).isDone()) {
                done++;
            }
        }
        int expected = Math.min(habit.timesPerWeek(), available);
        return new WeekProgress(done, expected);
    }

    private DayStatus statusOn(LocalDate date) {
        List<Entry> entries = entriesByDate.getOrDefault(date, List.of());
        if (entries.isEmpty()) {
            return DayStatus.NOT_DONE;
        }
        if (habit.kind() == HabitKind.CHECK) {
            boolean anyFull = entries.stream().anyMatch(entry -> !entry.minimal());
            return anyFull ? DayStatus.DONE : DayStatus.DONE_MINIMAL;
        }
        if (amountOn(date) + EPSILON >= habit.targetAmount()) {
            return DayStatus.DONE;
        }
        boolean anyMinimal = entries.stream().anyMatch(Entry::minimal);
        return anyMinimal ? DayStatus.DONE_MINIMAL : DayStatus.NOT_DONE;
    }

    private double amountOn(LocalDate date) {
        return entriesByDate.getOrDefault(date, List.of()).stream().mapToDouble(Entry::value).sum();
    }
}
