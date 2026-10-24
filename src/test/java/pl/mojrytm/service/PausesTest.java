package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.mojrytm.model.Pause;

class PausesTest {

    private static LocalDate day(int d) {
        return LocalDate.of(2026, 10, d);
    }

    @Test
    void noPausesMeansNoPausedDays() {
        assertFalse(new Pauses(List.of()).isPaused(day(10)));
    }

    @Test
    void bothEndsOfThePauseAreIncluded() {
        Pauses pauses = new Pauses(List.of(new Pause(day(10), day(12), "choroba")));

        assertFalse(pauses.isPaused(day(9)));
        assertTrue(pauses.isPaused(day(10)));
        assertTrue(pauses.isPaused(day(11)));
        assertTrue(pauses.isPaused(day(12)));
        assertFalse(pauses.isPaused(day(13)));
    }

    @Test
    void oneDayPauseCoversExactlyThatDay() {
        Pauses pauses = new Pauses(List.of(new Pause(day(10), day(10), "wyjazd")));

        assertFalse(pauses.isPaused(day(9)));
        assertTrue(pauses.isPaused(day(10)));
        assertFalse(pauses.isPaused(day(11)));
    }

    @Test
    void severalPausesAreAllConsidered() {
        Pauses pauses = new Pauses(List.of(
                new Pause(day(1), day(2), "choroba"),
                new Pause(day(20), day(22), "wyjazd"),
                new Pause(day(21), day(25), "wyjazd")));

        assertTrue(pauses.isPaused(day(2)));
        assertFalse(pauses.isPaused(day(10)));
        assertTrue(pauses.isPaused(day(23)));
        assertTrue(pauses.isPaused(day(25)));
        assertFalse(pauses.isPaused(day(26)));
    }
}
