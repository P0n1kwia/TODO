package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import pl.mojrytm.model.AppData;

class HabitChoiceTest {

    private final AppData data = UiTestData.sample();

    @Test
    void listStartsWithNoHabitThenActiveHabitsInDataOrder() {
        List<HabitChoice> choices = HabitChoice.list(data, null);

        assertEquals(List.of("Bez nawyku", "Spacer", "Woda", "Bieg"), choices.stream().map(HabitChoice::label).toList());
        assertNull(choices.get(0).habitId());
        assertEquals("woda", choices.get(2).habitId());
    }

    @Test
    void anArchivedHabitStaysOnTheListOnlyWhenItIsTheSelectedOne() {
        List<HabitChoice> choices = HabitChoice.list(data, "stary");

        assertEquals(List.of("Bez nawyku", "Spacer", "Woda", "Bieg", "Stary nawyk (zarchiwizowany)"),
                choices.stream().map(HabitChoice::label).toList());
    }

    @Test
    void findPicksTheChoiceOfThatHabitAndFallsBackToNoHabit() {
        List<HabitChoice> choices = HabitChoice.list(data, null);

        assertEquals("bieg", HabitChoice.find(choices, "bieg").habitId());
        assertNull(HabitChoice.find(choices, null).habitId());
        assertNull(HabitChoice.find(choices, "nie-ma").habitId());
        assertNull(HabitChoice.find(choices, "stary").habitId());
    }

    @Test
    void withoutHabitsOnlyNoHabitIsOffered() {
        assertEquals(1, HabitChoice.list(AppData.empty(), null).size());
    }

    @Test
    void itIsShownAsItsLabel() {
        assertEquals("Spacer", HabitChoice.list(data, null).get(1).toString());
    }
}
