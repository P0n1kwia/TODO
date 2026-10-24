package pl.mojrytm.service;

import java.time.LocalDate;
import java.time.temporal.WeekFields;

/** Tydzień poniedziałek–niedziela (ISO). */
public final class Weeks {

    private Weeks() {
    }

    public static LocalDate startOf(LocalDate date) {
        return date.with(WeekFields.ISO.dayOfWeek(), 1);
    }

    public static LocalDate endOf(LocalDate date) {
        return startOf(date).plusDays(6);
    }
}
