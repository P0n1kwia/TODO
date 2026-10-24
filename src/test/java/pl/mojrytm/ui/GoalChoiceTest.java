package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import pl.mojrytm.service.GoalOption;

class GoalChoiceTest {

    private static final List<GoalOption> OPTIONS = List.of(
            new GoalOption("s1", "Sen", "Spać 8 godzin", false),
            new GoalOption("c1", "Ciało", "Biegać", true));

    @Test
    void listStartsWithNoGoalAndKeepsTheOrderOfOptions() {
        List<GoalChoice> choices = GoalChoice.list(OPTIONS);

        assertEquals(List.of("Bez celu", "Sen › Spać 8 godzin", "Ciało › Biegać (zarchiwizowany)"),
                choices.stream().map(GoalChoice::label).toList());
        assertNull(choices.get(0).goalId());
        assertEquals("c1", choices.get(2).goalId());
    }

    @Test
    void emptyOptionsGiveOnlyNoGoal() {
        assertEquals(1, GoalChoice.list(List.of()).size());
    }

    @Test
    void findPicksTheChoiceOfThatGoal() {
        List<GoalChoice> choices = GoalChoice.list(OPTIONS);

        assertEquals("s1", GoalChoice.find(choices, "s1").goalId());
    }

    @Test
    void findFallsBackToNoGoal() {
        List<GoalChoice> choices = GoalChoice.list(OPTIONS);

        assertNull(GoalChoice.find(choices, null).goalId());
        assertNull(GoalChoice.find(choices, "nie-ma").goalId());
    }

    @Test
    void itIsShownAsItsLabel() {
        assertEquals("Bez celu", GoalChoice.list(List.of()).get(0).toString());
    }
}
