package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.model.Pause;
import pl.mojrytm.model.Schedule;

class HabitEditorTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 1);

    private final HabitEditor editor = new HabitEditor();
    private final PauseEditor pauseEditor = new PauseEditor();

    private static Habit check(String id, String name) {
        return new Habit(id, null, name, HabitKind.CHECK, Schedule.DAILY, null, null, null, null, DAY, false);
    }

    @Test
    void addAppendsAtTheEnd() {
        AppData data = editor.add(editor.add(AppData.empty(), check("a", "Spacer")), check("b", "Woda"));

        assertEquals(List.of("a", "b"), data.habits().stream().map(Habit::id).toList());
    }

    @Test
    void addingTheSameIdTwiceIsRejected() {
        AppData data = editor.add(AppData.empty(), check("a", "Spacer"));

        assertThrows(IllegalArgumentException.class, () -> editor.add(data, check("a", "Inny")));
    }

    @Test
    void updateReplacesInPlaceKeepingTheOrder() {
        AppData data = editor.add(editor.add(AppData.empty(), check("a", "Spacer")), check("b", "Woda"));

        AppData changed = editor.update(data, check("a", "Długi spacer"));

        assertEquals(List.of("Długi spacer", "Woda"), changed.habits().stream().map(Habit::name).toList());
    }

    @Test
    void updateCanChangeTheSchedule() {
        AppData data = editor.add(AppData.empty(), check("a", "Spacer"));
        Habit weekly = new Habit("a", null, "Spacer", HabitKind.CHECK, Schedule.WEEKLY, 3, null, null, null, DAY, false);

        assertEquals(Schedule.WEEKLY, editor.update(data, weekly).habits().get(0).schedule());
    }

    @Test
    void updateCannotChangeTheKindOrTheCreationDay() {
        AppData data = editor.add(AppData.empty(), check("a", "Spacer"));
        Habit otherKind = new Habit("a", null, "Spacer", HabitKind.COUNT, Schedule.DAILY, null, 5.0, null, null,
                DAY, false);
        Habit otherDay = new Habit("a", null, "Spacer", HabitKind.CHECK, Schedule.DAILY, null, null, null, null,
                DAY.minusDays(1), false);

        assertThrows(IllegalArgumentException.class, () -> editor.update(data, otherKind));
        assertThrows(IllegalArgumentException.class, () -> editor.update(data, otherDay));
    }

    @Test
    void updatingAnUnknownHabitIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> editor.update(AppData.empty(), check("x", "Nie ma")));
    }

    @Test
    void archivingKeepsTheHabitAndCanBeUndone() {
        AppData data = editor.add(AppData.empty(), check("a", "Spacer"));

        AppData archived = editor.setArchived(data, "a", true);
        AppData restored = editor.setArchived(archived, "a", false);

        assertTrue(archived.habits().get(0).archived());
        assertEquals(1, archived.habits().size());
        assertFalse(restored.habits().get(0).archived());
    }

    @Test
    void archivingTwiceChangesNothingAndUnknownIdIsRejected() {
        AppData archived = editor.setArchived(editor.add(AppData.empty(), check("a", "Spacer")), "a", true);

        assertSame(archived, editor.setArchived(archived, "a", true));
        assertThrows(IllegalArgumentException.class, () -> editor.setArchived(archived, "x", true));
    }

    @Test
    void pausesAreAddedInOrderAndRemovedByValue() {
        Pause first = new Pause(DAY, DAY.plusDays(2), "choroba");
        Pause second = new Pause(DAY.plusDays(10), DAY.plusDays(12), "wyjazd");

        AppData both = pauseEditor.add(pauseEditor.add(AppData.empty(), first), second);
        AppData withoutFirst = pauseEditor.remove(both, first);

        assertEquals(List.of(first, second), both.pauses());
        assertEquals(List.of(second), withoutFirst.pauses());
    }

    @Test
    void removingAMissingPauseChangesNothing() {
        AppData data = pauseEditor.add(AppData.empty(), new Pause(DAY, DAY, "choroba"));

        assertSame(data, pauseEditor.remove(data, new Pause(DAY, DAY, "wyjazd")));
    }

    @Test
    void identicalPausesAreRemovedOneAtATime() {
        Pause pause = new Pause(DAY, DAY, "choroba");
        AppData twice = pauseEditor.add(pauseEditor.add(AppData.empty(), pause), pause);

        assertEquals(1, pauseEditor.remove(twice, pause).pauses().size());
    }
}
