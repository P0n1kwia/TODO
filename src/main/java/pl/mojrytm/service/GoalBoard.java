package pl.mojrytm.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Area;
import pl.mojrytm.model.Goal;
import pl.mojrytm.model.Habit;

/**
 * Zestawienia hierarchii Obszar → Cel → Nawyk dla widoków. W każdej grupie aktywne pozycje
 * idą przed zarchiwizowanymi, a kolejność wewnątrz zachowuje kolejność z danych. Nawyk wskazujący
 * nieistniejący cel (np. po ręcznej edycji pliku) traktujemy jak nawyk bez celu.
 */
public final class GoalBoard {

    /** Wszystkie obszary z ich celami. */
    public List<AreaNode> areas(AppData data) {
        Map<String, Long> habitsPerGoal = data.habits().stream()
                .filter(habit -> !habit.archived() && habit.goalId() != null)
                .collect(Collectors.groupingBy(Habit::goalId, Collectors.counting()));
        return activeFirst(data.areas(), Area::archived).stream()
                .map(area -> new AreaNode(area, activeFirst(goalsOf(data, area), Goal::archived).stream()
                        .map(goal -> new GoalNode(goal, habitsPerGoal.getOrDefault(goal.id(), 0L).intValue()))
                        .toList()))
                .toList();
    }

    /** Aktywne nawyki pogrupowane według celu; ostatnia grupa (jeśli jest) to nawyki bez celu. */
    public List<HabitGroup> habitGroups(AppData data) {
        List<Habit> active = data.habits().stream().filter(habit -> !habit.archived()).toList();
        List<HabitGroup> groups = new ArrayList<>();
        List<Habit> unassigned = new ArrayList<>(active);
        for (AreaNode area : areas(data)) {
            for (GoalNode node : area.goals()) {
                List<Habit> habits = active.stream()
                        .filter(habit -> node.goal().id().equals(habit.goalId()))
                        .toList();
                if (!habits.isEmpty()) {
                    groups.add(new HabitGroup(option(area.area(), node.goal()), habits));
                    unassigned.removeAll(habits);
                }
            }
        }
        if (!unassigned.isEmpty()) {
            groups.add(new HabitGroup(null, List.copyOf(unassigned)));
        }
        return List.copyOf(groups);
    }

    /**
     * Cele do wyboru w oknie nawyku: aktywne cele aktywnych obszarów oraz {@code currentGoalId},
     * gdy nawyk jest już do niego przypisany (żeby edycja nie gubiła zarchiwizowanego celu).
     */
    public List<GoalOption> goalOptions(AppData data, String currentGoalId) {
        List<GoalOption> options = new ArrayList<>();
        for (AreaNode area : areas(data)) {
            for (GoalNode node : area.goals()) {
                GoalOption option = option(area.area(), node.goal());
                if (!option.archived() || node.goal().id().equals(currentGoalId)) {
                    options.add(option);
                }
            }
        }
        return List.copyOf(options);
    }

    /** Opis celu o podanym id; pusty, gdy id jest null albo celu nie ma. */
    public Optional<GoalOption> goalOf(AppData data, String goalId) {
        if (goalId == null) {
            return Optional.empty();
        }
        return data.goals().stream()
                .filter(goal -> goal.id().equals(goalId))
                .findFirst()
                .flatMap(goal -> data.areas().stream()
                        .filter(area -> area.id().equals(goal.areaId()))
                        .findFirst()
                        .map(area -> option(area, goal)));
    }

    private static GoalOption option(Area area, Goal goal) {
        return new GoalOption(goal.id(), area.name(), goal.name(), area.archived() || goal.archived());
    }

    private static List<Goal> goalsOf(AppData data, Area area) {
        return data.goals().stream().filter(goal -> goal.areaId().equals(area.id())).toList();
    }

    private static <T> List<T> activeFirst(List<T> items, Predicate<T> archived) {
        return Stream.concat(items.stream().filter(archived.negate()), items.stream().filter(archived)).toList();
    }
}
