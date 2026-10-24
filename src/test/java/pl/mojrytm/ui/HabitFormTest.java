package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.model.Schedule;
import pl.mojrytm.ui.HabitForm.Problem;

class HabitFormTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 24);

    private static HabitForm check(String name) {
        return new HabitForm(name, HabitKind.CHECK, false, 3, "", "", "", START);
    }

    private static HabitForm count(String target) {
        return new HabitForm("Woda", HabitKind.COUNT, false, 3, target, "szklanki", "", START);
    }

    @Test
    void aSimpleCheckHabitIsValidAndBecomesADailyHabit() {
        HabitForm form = check("  Spacer  ");

        assertEquals(Optional.empty(), form.problem());
        assertEquals(new Habit("id", null, "Spacer", HabitKind.CHECK, Schedule.DAILY, null, null, null, null, START, false),
                form.toHabit("id", null, false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void nameMustNotBeBlank(String name) {
        assertEquals(Optional.of(Problem.NAME_EMPTY), check(name).problem());
    }

    @Test
    void aMissingNameIsAProblemToo() {
        assertEquals(Optional.of(Problem.NAME_EMPTY), check(null).problem());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "0", "-3", "NaN"})
    void countHabitNeedsAPositiveTarget(String target) {
        assertEquals(Optional.of(Problem.TARGET_INVALID), count(target).problem());
    }

    @Test
    void targetWithACommaIsAccepted() {
        Habit habit = count("2,5").toHabit("id", null, false);

        assertEquals(2.5, habit.targetAmount());
        assertEquals("szklanki", habit.unit());
    }

    @Test
    void checkHabitIgnoresTargetAndUnitTypedBeforeSwitchingTheKind() {
        HabitForm form = new HabitForm("Spacer", HabitKind.CHECK, false, 3, "abc", "szklanki", "", START);

        Habit habit = form.toHabit("id", null, false);

        assertEquals(Optional.empty(), form.problem());
        assertEquals(null, habit.targetAmount());
        assertEquals(null, habit.unit());
    }

    @Test
    void weeklyHabitKeepsTheNumberOfDaysAndDailyDropsIt() {
        HabitForm weekly = new HabitForm("Bieg", HabitKind.CHECK, true, 4, "", "", "", START);
        HabitForm daily = new HabitForm("Bieg", HabitKind.CHECK, false, 4, "", "", "", START);

        assertEquals(Schedule.WEEKLY, weekly.toHabit("id", null, false).schedule());
        assertEquals(4, weekly.toHabit("id", null, false).timesPerWeek());
        assertEquals(null, daily.toHabit("id", null, false).timesPerWeek());
    }

    @Test
    void blankOptionalTextsBecomeNull() {
        HabitForm form = new HabitForm("Woda", HabitKind.COUNT, false, 3, "8", "  ", "  ", START);

        Habit habit = form.toHabit("id", null, false);

        assertEquals(null, habit.unit());
        assertEquals(null, habit.minimumVersion());
    }

    @Test
    void minimumVersionIsTrimmed() {
        HabitForm form = new HabitForm("Spacer", HabitKind.CHECK, false, 3, "", "", "  Wyjść przed dom  ", START);

        assertEquals("Wyjść przed dom", form.toHabit("id", null, false).minimumVersion());
    }

    @Test
    void startDateIsRequired() {
        assertEquals(Optional.of(Problem.START_MISSING),
                new HabitForm("Spacer", HabitKind.CHECK, false, 3, "", "", "", null).problem());
    }

    @Test
    void editingKeepsIdGoalAndArchiveFlag() {
        Habit original = new Habit("keep", "goal", "Woda", HabitKind.COUNT, Schedule.WEEKLY, 5, 8.0, "szklanki",
                "2 szklanki", START, true);

        Habit rebuilt = HabitForm.from(original).toHabit("keep", "goal", true);

        assertEquals(original, rebuilt);
    }

    @Test
    void anInvalidFormCannotBeTurnedIntoAHabit() {
        assertThrows(IllegalStateException.class, () -> check("").toHabit("id", null, false));
    }

    @ParameterizedTest
    @EnumSource(Problem.class)
    void everyProblemHasAMessage(Problem problem) {
        assertEquals(true, Messages.has("habit.form.problem." + problem.name()));
    }
}
