package pl.mojrytm.service;

import pl.mojrytm.model.Habit;

/** Wiersz widoku „Tydzień": nawyk, jego tydzień (kratki i wykonanie) oraz bieżąca seria. */
public record WeekRow(Habit habit, WeekView week, Streak streak) {
}
