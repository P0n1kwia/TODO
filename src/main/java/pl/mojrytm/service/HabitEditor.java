package pl.mojrytm.service;

import java.util.ArrayList;
import java.util.List;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Habit;

/**
 * Zmiany na liście nawyków. Nawyków nie usuwamy, tylko archiwizujemy. Rodzaj nawyku i dzień
 * założenia są stałe po utworzeniu, bo od nich zależy znaczenie istniejących wpisów.
 * Naruszenia to błąd programisty ({@link IllegalArgumentException}): UI nie oferuje takich zmian.
 */
public final class HabitEditor {

    public AppData add(AppData data, Habit habit) {
        if (indexOf(data, habit.id()) >= 0) {
            throw new IllegalArgumentException("Nawyk o takim id już istnieje: " + habit.id());
        }
        requireGoalExists(data, habit);
        List<Habit> habits = new ArrayList<>(data.habits());
        habits.add(habit);
        return data.withHabits(habits);
    }

    /** Podmienia nawyk o tym samym id (nazwa, cel nadrzędny, harmonogram, cel ilościowy, wersja minimalna). */
    public AppData update(AppData data, Habit updated) {
        int index = requireIndex(data, updated.id());
        requireGoalExists(data, updated);
        Habit current = data.habits().get(index);
        if (current.kind() != updated.kind()) {
            throw new IllegalArgumentException("Rodzaju nawyku nie można zmienić");
        }
        if (!current.created().equals(updated.created())) {
            throw new IllegalArgumentException("Dnia założenia nawyku nie można zmienić");
        }
        List<Habit> habits = new ArrayList<>(data.habits());
        habits.set(index, updated);
        return data.withHabits(habits);
    }

    public AppData setArchived(AppData data, String habitId, boolean archived) {
        int index = requireIndex(data, habitId);
        Habit current = data.habits().get(index);
        if (current.archived() == archived) {
            return data;
        }
        List<Habit> habits = new ArrayList<>(data.habits());
        habits.set(index, current.withArchived(archived));
        return data.withHabits(habits);
    }

    private static void requireGoalExists(AppData data, Habit habit) {
        if (habit.goalId() != null && data.goals().stream().noneMatch(goal -> goal.id().equals(habit.goalId()))) {
            throw new IllegalArgumentException("Nie ma celu o id: " + habit.goalId());
        }
    }

    private static int requireIndex(AppData data, String habitId) {
        int index = indexOf(data, habitId);
        if (index < 0) {
            throw new IllegalArgumentException("Nie ma nawyku o id: " + habitId);
        }
        return index;
    }

    private static int indexOf(AppData data, String habitId) {
        List<Habit> habits = data.habits();
        for (int i = 0; i < habits.size(); i++) {
            if (habits.get(i).id().equals(habitId)) {
                return i;
            }
        }
        return -1;
    }
}
