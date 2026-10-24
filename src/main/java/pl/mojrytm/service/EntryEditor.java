package pl.mojrytm.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Entry;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.model.Ids;
import pl.mojrytm.service.EntryRejectedException.Reason;

/**
 * Zmiany wpisów z pilnowaniem reguł. Każda metoda dostaje migawkę danych i zwraca nową
 * (albo tę samą, gdy nic się nie zmieniło); naruszenie reguły to {@link EntryRejectedException}.
 *
 * <p>Edytor utrzymuje najwyżej jeden wpis na nawyk na dzień. Wsteczne uzupełnianie jest dozwolone,
 * dzień z przyszłości, dzień przed założeniem nawyku i nawyk zarchiwizowany nie.
 * Dni w pauzie można zaznaczać.
 */
public final class EntryEditor {

    private final LogicalClock clock;
    private final Supplier<String> idGenerator;

    public EntryEditor(LogicalClock clock) {
        this(clock, Ids::newId);
    }

    EntryEditor(LogicalClock clock, Supplier<String> idGenerator) {
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    /** Zaznacza albo odznacza dzień nawyku typu CHECK; {@code minimal} oznacza wersję minimalną. */
    public AppData setChecked(AppData data, String habitId, LocalDate date, boolean checked, boolean minimal) {
        requireEditable(data, habitId, date, HabitKind.CHECK);
        return replaceDay(data, habitId, date, checked ? 1.0 : null, minimal);
    }

    /** Ustawia ilość z dnia dla nawyku COUNT. Zero bez wersji minimalnej usuwa wpis dnia. */
    public AppData setAmount(AppData data, String habitId, LocalDate date, double amount, boolean minimal) {
        requireEditable(data, habitId, date, HabitKind.COUNT);
        requireFinite(amount);
        if (amount < 0) {
            throw new EntryRejectedException(Reason.INVALID_AMOUNT);
        }
        boolean remove = amount == 0 && !minimal;
        return replaceDay(data, habitId, date, remove ? null : amount, minimal);
    }

    /** Dodaje (lub odejmuje, nie schodząc poniżej zera) ilość do sumy z dnia. */
    public AppData addAmount(AppData data, String habitId, LocalDate date, double delta) {
        requireEditable(data, habitId, date, HabitKind.COUNT);
        requireFinite(delta);
        double current = 0;
        boolean minimal = false;
        for (Entry entry : data.entries()) {
            if (entry.habitId().equals(habitId) && entry.date().equals(date)) {
                current += entry.value();
                minimal |= entry.minimal();
            }
        }
        double next = Math.max(0, roundForStorage(current + delta));
        return setAmount(data, habitId, date, next, minimal);
    }

    private void requireEditable(AppData data, String habitId, LocalDate date, HabitKind expectedKind) {
        Habit habit = data.habits().stream()
                .filter(candidate -> candidate.id().equals(habitId))
                .findFirst()
                .orElseThrow(() -> new EntryRejectedException(Reason.HABIT_NOT_FOUND));
        if (habit.archived()) {
            throw new EntryRejectedException(Reason.HABIT_ARCHIVED);
        }
        if (habit.kind() != expectedKind) {
            throw new EntryRejectedException(Reason.WRONG_KIND);
        }
        if (date.isAfter(clock.today(data.settings()))) {
            throw new EntryRejectedException(Reason.FUTURE_DAY);
        }
        if (date.isBefore(habit.created())) {
            throw new EntryRejectedException(Reason.BEFORE_HABIT_CREATED);
        }
    }

    private static void requireFinite(double amount) {
        if (!Double.isFinite(amount)) {
            throw new EntryRejectedException(Reason.INVALID_AMOUNT);
        }
    }

    /** Sumy dziesiętne (0.1 + 0.2) nie mają trafiać do pliku jako 0.30000000000000004. */
    private static double roundForStorage(double value) {
        return Math.round(value * 1_000_000d) / 1_000_000d;
    }

    /**
     * Zastępuje wszystkie wpisy nawyku z danego dnia jednym wpisem o wartości {@code value}
     * (null = brak wpisu). Nowy wpis zajmuje miejsce pierwszego dotychczasowego i dziedziczy jego
     * id, dzięki czemu plik zmienia się minimalnie.
     */
    private AppData replaceDay(AppData data, String habitId, LocalDate date, Double value, boolean minimal) {
        List<Entry> result = new ArrayList<>();
        boolean placed = false;
        for (Entry entry : data.entries()) {
            if (!entry.habitId().equals(habitId) || !entry.date().equals(date)) {
                result.add(entry);
                continue;
            }
            if (!placed && value != null) {
                result.add(new Entry(entry.id(), habitId, date, value, minimal));
                placed = true;
            }
        }
        if (!placed && value != null) {
            result.add(new Entry(idGenerator.get(), habitId, date, value, minimal));
        }
        return result.equals(data.entries()) ? data : data.withEntries(result);
    }
}
