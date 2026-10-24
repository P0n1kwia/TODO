package pl.mojrytm.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Habit;

/** Pozycja listy wyboru nawyku przy notatce. {@code habitId} równe null to "bez nawyku". */
record HabitChoice(String habitId, String label) {

    /**
     * Lista do wyboru: "bez nawyku", potem aktywne nawyki w kolejności z danych oraz nawyk {@code keepHabitId},
     * jeśli jest już wybrany, choćby zarchiwizowany (z dopiskiem), żeby nie znikał z zapisanej notatki.
     */
    static List<HabitChoice> list(AppData data, String keepHabitId) {
        List<HabitChoice> choices = new ArrayList<>();
        choices.add(new HabitChoice(null, Messages.get("journal.fields.habit.none")));
        for (Habit habit : data.habits()) {
            if (!habit.archived()) {
                choices.add(new HabitChoice(habit.id(), habit.name()));
            } else if (habit.id().equals(keepHabitId)) {
                choices.add(new HabitChoice(habit.id(), Messages.format("journal.fields.habit.archived", habit.name())));
            }
        }
        return List.copyOf(choices);
    }

    /** Pozycja dla podanego nawyku; gdy jej nie ma (albo habitId to null), pierwsza czyli "bez nawyku". */
    static HabitChoice find(List<HabitChoice> choices, String habitId) {
        return choices.stream()
                .filter(choice -> Objects.equals(choice.habitId(), habitId))
                .findFirst()
                .orElse(choices.get(0));
    }

    @Override
    public String toString() {
        return label;
    }
}
