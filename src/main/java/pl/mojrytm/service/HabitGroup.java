package pl.mojrytm.service;

import java.util.List;
import pl.mojrytm.model.Habit;

/** Aktywne nawyki jednego celu. {@code goal} równe null oznacza nawyki bez celu. */
public record HabitGroup(GoalOption goal, List<Habit> habits) {
}
