package pl.mojrytm.service;

import pl.mojrytm.model.Habit;

/** Wiersz widoku „Dziś": nawyk, dzisiejsza kratka, seria i wykonanie bieżącego tygodnia. */
public record HabitRow(Habit habit, DayCell today, Streak streak, WeekProgress week) {
}
