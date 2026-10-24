package pl.mojrytm.service;

import java.time.LocalDate;
import java.util.List;

/** Tydzień jednego nawyku: siedem kratek od poniedziałku i wykonanie tygodnia. */
public record WeekView(LocalDate weekStart, List<DayCell> days, WeekProgress progress) {
}
