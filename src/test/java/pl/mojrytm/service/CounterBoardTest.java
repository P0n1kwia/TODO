package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Counter;

class CounterBoardTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);

    private final CounterBoard board = new CounterBoard();

    private static AppData with(Counter... counters) {
        return AppData.empty().withCounters(List.of(counters));
    }

    @Test
    void theStartDayIsZeroAndTheNextDayIsOne() {
        AppData data = with(new Counter("a", "Dziś", TODAY), new Counter("b", "Wczoraj", TODAY.minusDays(1)));

        assertEquals(List.of(0, 1), board.rows(data, TODAY).stream().map(CounterRow::days).toList());
    }

    @Test
    void countsDaysAcrossMonthsAndYearsAndLeapDays() {
        AppData data = with(new Counter("a", "Od września", LocalDate.of(2026, 9, 1)),
                new Counter("b", "Od zeszłego roku", LocalDate.of(2025, 10, 24)),
                new Counter("c", "Przez 29 lutego", LocalDate.of(2024, 2, 28)));

        assertEquals(List.of(53, 365, 969), board.rows(data, TODAY).stream().map(CounterRow::days).toList());
    }

    @Test
    void aStartInTheFutureCountsAsZero() {
        AppData data = with(new Counter("a", "Jutro", TODAY.plusDays(1)));

        assertEquals(0, board.rows(data, TODAY).get(0).days());
    }

    @Test
    void rowsKeepTheOrderOfTheDataAndTheCounter() {
        Counter first = new Counter("a", "Pierwszy", TODAY.minusDays(5));
        Counter second = new Counter("b", "Drugi", TODAY.minusDays(50));

        List<CounterRow> rows = board.rows(with(first, second), TODAY);

        assertEquals(List.of(new CounterRow(first, 5), new CounterRow(second, 50)), rows);
    }

    @Test
    void noCountersGiveNoRows() {
        assertEquals(List.of(), board.rows(AppData.empty(), TODAY));
    }
}
