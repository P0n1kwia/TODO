package pl.mojrytm.service;

import java.time.LocalDate;
import java.util.List;
import pl.mojrytm.model.Pause;

/** Pauzy (choroba, wyjazd) obejmujące wszystkie nawyki; oba końce przedziału włącznie. */
public final class Pauses {

    private final List<Pause> pauses;

    public Pauses(List<Pause> pauses) {
        this.pauses = List.copyOf(pauses);
    }

    public boolean isPaused(LocalDate date) {
        for (Pause pause : pauses) {
            if (!date.isBefore(pause.from()) && !date.isAfter(pause.to())) {
                return true;
            }
        }
        return false;
    }
}
