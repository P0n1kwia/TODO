package pl.mojrytm.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pl.mojrytm.service.GoalOption;

/** Pozycja listy wyboru celu w oknie nawyku. {@code goalId} równe null to "bez celu". */
record GoalChoice(String goalId, String label) {

    /** Lista do wyboru: najpierw "bez celu", potem podane cele w danej kolejności. */
    static List<GoalChoice> list(List<GoalOption> options) {
        List<GoalChoice> choices = new ArrayList<>();
        choices.add(new GoalChoice(null, Messages.get("dialog.habit.goal.none")));
        for (GoalOption option : options) {
            choices.add(new GoalChoice(option.goalId(), Formats.goalLabel(option)));
        }
        return List.copyOf(choices);
    }

    /** Pozycja dla podanego celu; gdy jej nie ma (albo goalId to null), pierwsza czyli "bez celu". */
    static GoalChoice find(List<GoalChoice> choices, String goalId) {
        return choices.stream()
                .filter(choice -> Objects.equals(choice.goalId(), goalId))
                .findFirst()
                .orElse(choices.get(0));
    }

    @Override
    public String toString() {
        return label;
    }
}
