package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Habit;
import pl.mojrytm.model.HabitKind;
import pl.mojrytm.model.Schedule;
import pl.mojrytm.storage.JsonStorage;
import pl.mojrytm.storage.LoadResult;

class AppModelTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock("2026-10-24T10:00:00Z");
    private JsonStorage storage;
    private AppModel model;

    private static Habit habit() {
        return new Habit("h1", null, "Spacer", HabitKind.CHECK, Schedule.DAILY, null, null, null, null,
                LocalDate.of(2026, 1, 1), false);
    }

    @BeforeEach
    void setUp() {
        storage = new JsonStorage(dir, clock);
        model = new AppModel(AppData.empty().withHabits(List.of(habit())), storage, clock);
    }

    @AfterEach
    void tearDown() throws IOException {
        model.shutdown();
    }

    @Test
    void startsWithTheGivenDataAndTodaysLogicalDay() {
        assertEquals(1, model.data().habits().size());
        assertEquals(TODAY, model.today());
    }

    @Test
    void updateChangesTheDataAndNotifiesObservers() {
        List<AppData> seen = new ArrayList<>();
        model.dataProperty().addListener((observable, previous, current) -> seen.add(current));

        boolean changed = model.update(data -> model.entries().setChecked(data, "h1", TODAY, true, false));

        assertTrue(changed);
        assertEquals(1, model.data().entries().size());
        assertEquals(List.of(model.data()), seen);
    }

    @Test
    void ruleViolationShowsAMessageAndKeepsTheData() {
        AppData before = model.data();

        boolean changed = model.update(data -> model.entries().setChecked(data, "h1", TODAY.plusDays(1), true, false));

        assertFalse(changed);
        assertEquals(before, model.data());
        assertEquals(new StatusMessage(Messages.get("error.entry.FUTURE_DAY"), true), model.statusProperty().get());
    }

    @Test
    void programmingErrorsAreNotSwallowed() {
        assertThrows(IllegalArgumentException.class,
                () -> model.update(data -> model.habits().add(data, habit())));
    }

    @Test
    void statusMessagesCanBeShownAndCleared() {
        model.showInfo("Zapisano");
        assertEquals(new StatusMessage("Zapisano", false), model.statusProperty().get());

        model.clearStatus();
        assertNull(model.statusProperty().get());
    }

    @Test
    void newDayIsNoticedAfterTheLogicalDayChanges() {
        List<LocalDate> seen = new ArrayList<>();
        model.todayProperty().addListener((observable, previous, current) -> seen.add(current));

        model.refreshToday();
        assertEquals(List.of(), seen);

        clock.advance(Duration.ofHours(18)); // 04:00 następnego dnia
        model.refreshToday();

        assertEquals(List.of(TODAY.plusDays(1)), seen);
    }

    @Test
    void justBeforeFourInTheMorningItIsStillYesterday() {
        clock.advance(Duration.ofHours(17).plusMinutes(59)); // 03:59 następnego dnia

        model.refreshToday();

        assertEquals(TODAY, model.today());
    }

    @Test
    void shutdownWritesPendingChangesToDisk() throws IOException {
        model.update(data -> model.entries().setChecked(data, "h1", TODAY, true, false));

        model.shutdown();

        assertTrue(Files.exists(storage.dataFile()));
        LoadResult.Loaded loaded = (LoadResult.Loaded) storage.load();
        assertEquals(1, loaded.data().entries().size());
    }
}
