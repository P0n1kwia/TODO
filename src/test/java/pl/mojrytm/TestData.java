package pl.mojrytm;

import java.time.LocalDate;
import java.util.List;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Area;
import pl.mojrytm.model.CheckIn;
import pl.mojrytm.model.Counter;
import pl.mojrytm.model.Entry;
import pl.mojrytm.model.Goal;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.model.Note;
import pl.mojrytm.model.NoteType;
import pl.mojrytm.model.Pause;
import pl.mojrytm.model.Schedule;
import pl.mojrytm.model.Settings;
import pl.mojrytm.model.Task;
import pl.mojrytm.model.TaskStatus;
import pl.mojrytm.model.WheelRating;

/** Przykładowe dane do testów. Stałe identyfikatory, żeby wyniki były powtarzalne. */
public final class TestData {

    private TestData() {
    }

    /** Po jednej encji każdego rodzaju; odpowiada plikowi sample-v1.json. */
    public static AppData sampleData() {
        return new AppData(
                AppData.CURRENT_SCHEMA_VERSION,
                new Settings(4, null),
                List.of(new Area("area-1", "Sen")),
                List.of(new Goal("goal-1", "area-1", "Kłaść się przed 23:00", false)),
                List.of(
                        new Habit("habit-1", "goal-1", "Wieczorne rozciąganie", HabitKind.CHECK, Schedule.DAILY,
                                null, null, null, "Jedna pozycja przez minutę", LocalDate.of(2026, 10, 1), false),
                        new Habit("habit-2", null, "Woda", HabitKind.COUNT, Schedule.WEEKLY,
                                5, 8.0, "szklanki", null, LocalDate.of(2026, 10, 5), true)),
                List.of(
                        new Entry("entry-1", "habit-1", LocalDate.of(2026, 10, 2), 1.0, true),
                        new Entry("entry-2", "habit-2", LocalDate.of(2026, 10, 6), 2.5, false)),
                List.of(new Task("task-1", "Umówić wizytę u dentysty", TaskStatus.TODAY,
                        LocalDate.of(2026, 10, 25), null)),
                List.of(new Note("note-1", LocalDate.of(2026, 10, 24), NoteType.DAILY,
                        "Dobry dzień, mało snu.", List.of("sen", "energia"), "habit-1")),
                List.of(new CheckIn(LocalDate.of(2026, 10, 24), 4, 3)),
                List.of(new Counter("counter-1", "Dni bez słodyczy", LocalDate.of(2026, 9, 1))),
                List.of(new Pause(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12), "choroba")),
                List.of(new WheelRating(LocalDate.of(2026, 10, 1), "area-1", 6)));
    }
}
