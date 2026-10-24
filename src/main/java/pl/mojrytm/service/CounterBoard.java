package pl.mojrytm.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Counter;

/** Zestawienie liczników dla widoku, w kolejności z danych. UI niczego nie liczy. */
public final class CounterBoard {

    public List<CounterRow> rows(AppData data, LocalDate today) {
        return data.counters().stream()
                .map(counter -> new CounterRow(counter, daysSince(counter, today)))
                .toList();
    }

    /**
     * Liczba pełnych dni od startu: w dniu startu 0, nazajutrz 1. Start w przyszłości (np. po cofnięciu
     * zegara albo ręcznej edycji pliku) daje 0, bo licznik jeszcze nie ruszył.
     */
    static int daysSince(Counter counter, LocalDate today) {
        long days = ChronoUnit.DAYS.between(counter.startDate(), today);
        return (int) Math.max(0, Math.min(days, Integer.MAX_VALUE));
    }
}
