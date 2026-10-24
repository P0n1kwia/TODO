package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Area;
import pl.mojrytm.model.Goal;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.model.Schedule;

class GoalBoardTest {

    private final GoalBoard board = new GoalBoard();

    private static Habit habit(String id, String goalId, boolean archived) {
        return new Habit(id, goalId, id, HabitKind.CHECK, Schedule.DAILY, null, null, null, null,
                LocalDate.of(2026, 1, 1), archived);
    }

    private static List<String> ids(List<Habit> habits) {
        return habits.stream().map(Habit::id).toList();
    }

    /**
     * Obszary: Sen (aktywny, cele s1 i s2 oraz zarchiwizowany s0), Ciało (aktywny, cel c1), Stary (zarchiwizowany,
     * cel o1). Dane celowo podane w kolejności "zarchiwizowane przed aktywnymi".
     */
    private static AppData tree() {
        return AppData.empty()
                .withAreas(List.of(new Area("old", "Stary", true), new Area("sen", "Sen", false),
                        new Area("cialo", "Ciało", false)))
                .withGoals(List.of(
                        new Goal("o1", "old", "Dawny cel", false),
                        new Goal("s0", "sen", "Wygasły", true),
                        new Goal("s1", "sen", "Spać 8 godzin", false),
                        new Goal("c1", "cialo", "Biegać", false),
                        new Goal("s2", "sen", "Kłaść się przed 23", false)));
    }

    @Test
    void areasListActiveOnesFirstWithTheirGoalsActiveFirst() {
        List<AreaNode> areas = board.areas(tree());

        assertEquals(List.of("Sen", "Ciało", "Stary"), areas.stream().map(node -> node.area().name()).toList());
        assertEquals(List.of("Spać 8 godzin", "Kłaść się przed 23", "Wygasły"),
                areas.get(0).goals().stream().map(node -> node.goal().name()).toList());
        assertEquals(List.of("Dawny cel"), areas.get(2).goals().stream().map(node -> node.goal().name()).toList());
    }

    @Test
    void goalNodesCountOnlyActiveHabits() {
        AppData data = tree().withHabits(List.of(
                habit("a", "s1", false), habit("b", "s1", false), habit("c", "s1", true),
                habit("d", "c1", false), habit("e", null, false), habit("f", "nie-ma", false)));

        List<GoalNode> sen = board.areas(data).get(0).goals();

        assertEquals(List.of(2, 0, 0), sen.stream().map(GoalNode::activeHabits).toList());
        assertEquals(1, board.areas(data).get(1).goals().get(0).activeHabits());
    }

    @Test
    void areasWithoutDataAreEmpty() {
        assertEquals(List.of(), board.areas(AppData.empty()));
        assertEquals(List.of(), board.habitGroups(AppData.empty()));
    }

    @Test
    void habitGroupsFollowAreaAndGoalOrderAndEndWithHabitsWithoutGoal() {
        AppData data = tree().withHabits(List.of(
                habit("bez-celu", null, false), habit("bieg", "c1", false), habit("sen-b", "s2", false),
                habit("sen-a", "s1", false), habit("dawny", "o1", false), habit("wygasly", "s0", false)));

        List<HabitGroup> groups = board.habitGroups(data);

        assertEquals(List.of("Spać 8 godzin", "Kłaść się przed 23", "Wygasły", "Biegać", "Dawny cel"),
                groups.subList(0, 5).stream().map(group -> group.goal().goalName()).toList());
        assertEquals(List.of("sen-a"), ids(groups.get(0).habits()));
        assertEquals(List.of("sen-b"), ids(groups.get(1).habits()));
        assertNull(groups.get(5).goal());
        assertEquals(List.of("bez-celu"), ids(groups.get(5).habits()));
        assertEquals(6, groups.size());
    }

    @Test
    void habitGroupsSkipArchivedHabitsAndEmptyGoals() {
        AppData data = tree().withHabits(List.of(habit("a", "s1", true), habit("b", "c1", false)));

        List<HabitGroup> groups = board.habitGroups(data);

        assertEquals(1, groups.size());
        assertEquals(List.of("b"), ids(groups.get(0).habits()));
    }

    @Test
    void habitWithAnUnknownGoalLandsAmongHabitsWithoutGoal() {
        AppData data = tree().withHabits(List.of(habit("a", "nie-ma", false)));

        List<HabitGroup> groups = board.habitGroups(data);

        assertEquals(1, groups.size());
        assertNull(groups.get(0).goal());
        assertEquals(List.of("a"), ids(groups.get(0).habits()));
    }

    @Test
    void goalOptionsOfferOnlyActiveGoalsOfActiveAreas() {
        List<GoalOption> options = board.goalOptions(tree(), null);

        assertEquals(List.of("s1", "s2", "c1"), options.stream().map(GoalOption::goalId).toList());
        assertEquals(new GoalOption("s1", "Sen", "Spać 8 godzin", false), options.get(0));
    }

    @Test
    void goalOptionsKeepTheCurrentGoalEvenWhenArchived() {
        List<GoalOption> inArchivedGoal = board.goalOptions(tree(), "s0");
        List<GoalOption> inArchivedArea = board.goalOptions(tree(), "o1");

        assertEquals(List.of("s1", "s2", "s0", "c1"), inArchivedGoal.stream().map(GoalOption::goalId).toList());
        assertTrue(inArchivedGoal.get(2).archived());
        assertEquals(List.of("s1", "s2", "c1", "o1"), inArchivedArea.stream().map(GoalOption::goalId).toList());
        assertTrue(inArchivedArea.get(3).archived());
    }

    @Test
    void goalOfDescribesAGoalWithItsAreaAndArchiveState() {
        assertEquals(Optional.of(new GoalOption("s1", "Sen", "Spać 8 godzin", false)), board.goalOf(tree(), "s1"));
        assertTrue(board.goalOf(tree(), "s0").orElseThrow().archived());
        assertTrue(board.goalOf(tree(), "o1").orElseThrow().archived());
    }

    @Test
    void goalOfIsEmptyForNoGoalOrAnUnknownOne() {
        assertFalse(board.goalOf(tree(), null).isPresent());
        assertFalse(board.goalOf(tree(), "nie-ma").isPresent());
    }

    @Test
    void goalWhoseAreaIsMissingIsLeftOutOfTheTree() {
        AppData data = tree().withGoals(List.of(new Goal("g", "nie-ma-obszaru", "Sierota", false)));

        assertTrue(board.areas(data).stream().allMatch(node -> node.goals().isEmpty()));
        assertFalse(board.goalOf(data, "g").isPresent());
    }
}
