package pl.mojrytm.service;

import java.time.LocalDate;

/**
 * Jedna kratka w widoku nawyku (dzień). Wszystko, czego UI potrzebuje, żeby ją narysować.
 *
 * @param amount        suma wartości wpisów z tego dnia
 * @param paused        dzień objęty pauzą (nie wlicza się do serii i procentów)
 * @param beforeCreated dzień przed założeniem nawyku (nie liczy się nigdzie)
 * @param future        dzień po dzisiejszym (nie można go zaznaczyć)
 */
public record DayCell(
        LocalDate date, DayStatus status, double amount, boolean paused, boolean beforeCreated, boolean future) {

    /** Czy można zmieniać wpisy tego dnia. */
    public boolean editable() {
        return !beforeCreated && !future;
    }
}
