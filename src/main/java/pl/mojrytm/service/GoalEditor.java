package pl.mojrytm.service;

import java.util.ArrayList;
import java.util.List;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Goal;

/**
 * Zmiany na liście celów. Celów nie usuwamy, tylko archiwizujemy; cel zostaje w swoim obszarze
 * (przenoszenia między obszarami nie ma). Nazwy są unikalne w obszarze, bez względu na wielkość
 * liter. Naruszenia to błąd programisty ({@link IllegalArgumentException}): UI sprawdza je
 * wcześniej przez {@link #nameTaken}.
 */
public final class GoalEditor {

    /** Czy inny cel niż {@code exceptGoalId} (null = żaden wyjątek) ma w tym obszarze już taką nazwę. */
    public boolean nameTaken(AppData data, String areaId, String name, String exceptGoalId) {
        return data.goals().stream()
                .anyMatch(goal -> goal.areaId().equals(areaId) && !goal.id().equals(exceptGoalId)
                        && Names.same(goal.name(), name));
    }

    public AppData add(AppData data, Goal goal) {
        if (indexOf(data, goal.id()) >= 0) {
            throw new IllegalArgumentException("Cel o takim id już istnieje: " + goal.id());
        }
        if (data.areas().stream().noneMatch(area -> area.id().equals(goal.areaId()))) {
            throw new IllegalArgumentException("Nie ma obszaru o id: " + goal.areaId());
        }
        if (nameTaken(data, goal.areaId(), goal.name(), null)) {
            throw new IllegalArgumentException("Cel o takiej nazwie już istnieje w tym obszarze: " + goal.name());
        }
        List<Goal> goals = new ArrayList<>(data.goals());
        goals.add(goal);
        return data.withGoals(goals);
    }

    public AppData rename(AppData data, String goalId, String newName) {
        int index = requireIndex(data, goalId);
        Goal current = data.goals().get(index);
        if (nameTaken(data, current.areaId(), newName, goalId)) {
            throw new IllegalArgumentException("Cel o takiej nazwie już istnieje w tym obszarze: " + newName);
        }
        List<Goal> goals = new ArrayList<>(data.goals());
        goals.set(index, new Goal(goalId, current.areaId(), newName, current.archived()));
        return data.withGoals(goals);
    }

    public AppData setArchived(AppData data, String goalId, boolean archived) {
        int index = requireIndex(data, goalId);
        Goal current = data.goals().get(index);
        if (current.archived() == archived) {
            return data;
        }
        List<Goal> goals = new ArrayList<>(data.goals());
        goals.set(index, new Goal(current.id(), current.areaId(), current.name(), archived));
        return data.withGoals(goals);
    }

    private static int requireIndex(AppData data, String goalId) {
        int index = indexOf(data, goalId);
        if (index < 0) {
            throw new IllegalArgumentException("Nie ma celu o id: " + goalId);
        }
        return index;
    }

    private static int indexOf(AppData data, String goalId) {
        List<Goal> goals = data.goals();
        for (int i = 0; i < goals.size(); i++) {
            if (goals.get(i).id().equals(goalId)) {
                return i;
            }
        }
        return -1;
    }
}
