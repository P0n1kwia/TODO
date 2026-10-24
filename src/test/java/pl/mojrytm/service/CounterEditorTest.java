package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Counter;

class CounterEditorTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 1);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);

    private final CounterEditor editor = new CounterEditor();

    private static Counter counter(String id, String name) {
        return new Counter(id, name, START);
    }

    private static AppData with(Counter... counters) {
        return AppData.empty().withCounters(List.of(counters));
    }

    @Test
    void addAppendsAtTheEnd() {
        AppData data = editor.add(editor.add(AppData.empty(), counter("a", "Bez słodyczy")), counter("b", "Bez kawy"));

        assertEquals(List.of("a", "b"), data.counters().stream().map(Counter::id).toList());
    }

    @Test
    void addingTheSameIdTwiceIsRejected() {
        AppData data = with(counter("a", "Bez słodyczy"));

        assertThrows(IllegalArgumentException.class, () -> editor.add(data, counter("a", "Inny")));
    }

    @Test
    void renameKeepsTheStartAndTheOrder() {
        AppData data = with(counter("a", "Bez słodyczy"), counter("b", "Bez kawy"));

        AppData renamed = editor.rename(data, "a", "Bez cukru");

        assertEquals(List.of(counter("a", "Bez cukru"), counter("b", "Bez kawy")), renamed.counters());
    }

    @Test
    void restartMovesTheStartToTodayKeepingTheName() {
        AppData data = with(counter("a", "Bez słodyczy"), counter("b", "Bez kawy"));

        AppData restarted = editor.restart(data, "a", TODAY);

        assertEquals(new Counter("a", "Bez słodyczy", TODAY), restarted.counters().get(0));
        assertEquals(counter("b", "Bez kawy"), restarted.counters().get(1));
    }

    @Test
    void restartingACounterThatStartsTodayChangesNothing() {
        AppData data = with(new Counter("a", "Bez słodyczy", TODAY));

        assertSame(data, editor.restart(data, "a", TODAY));
    }

    @Test
    void removeDropsOnlyThatCounter() {
        AppData data = with(counter("a", "Bez słodyczy"), counter("b", "Bez kawy"));

        assertEquals(List.of(counter("b", "Bez kawy")), editor.remove(data, "a").counters());
    }

    @Test
    void unknownCountersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> editor.rename(AppData.empty(), "x", "Nazwa"));
        assertThrows(IllegalArgumentException.class, () -> editor.restart(AppData.empty(), "x", TODAY));
        assertThrows(IllegalArgumentException.class, () -> editor.remove(AppData.empty(), "x"));
    }
}
