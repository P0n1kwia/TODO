package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.mojrytm.service.Fixtures.FRI;
import static pl.mojrytm.service.Fixtures.LONG_AGO;
import static pl.mojrytm.service.Fixtures.SAT;
import static pl.mojrytm.service.Fixtures.SUN;
import static pl.mojrytm.service.Fixtures.THU;
import static pl.mojrytm.service.Fixtures.TODAY;
import static pl.mojrytm.service.Fixtures.WED;
import static pl.mojrytm.service.Fixtures.amount;
import static pl.mojrytm.service.Fixtures.checkDaily;
import static pl.mojrytm.service.Fixtures.countDaily;
import static pl.mojrytm.service.Fixtures.done;
import static pl.mojrytm.service.Fixtures.doneBy;
import static pl.mojrytm.service.Fixtures.minimal;
import static pl.mojrytm.service.Fixtures.minimalAmount;
import static pl.mojrytm.service.Fixtures.pause;
import static pl.mojrytm.service.Fixtures.pauses;
import static pl.mojrytm.service.Fixtures.tracker;

import org.junit.jupiter.api.Test;

class HabitTrackerDayTest {

    private static DayStatus statusOn(HabitTracker tracker) {
        return tracker.day(FRI, TODAY).status();
    }

    @Test
    void checkWithoutEntryIsNotDone() {
        assertEquals(DayStatus.NOT_DONE, statusOn(tracker(checkDaily(LONG_AGO))));
    }

    @Test
    void checkWithEntryIsDone() {
        assertEquals(DayStatus.DONE, statusOn(tracker(checkDaily(LONG_AGO), done(FRI))));
    }

    @Test
    void checkWithOnlyMinimalEntryIsDoneMinimalButStillDone() {
        DayStatus status = statusOn(tracker(checkDaily(LONG_AGO), minimal(FRI)));

        assertEquals(DayStatus.DONE_MINIMAL, status);
        assertTrue(status.isDone());
    }

    @Test
    void checkWithMinimalAndFullEntryIsFullyDone() {
        assertEquals(DayStatus.DONE, statusOn(tracker(checkDaily(LONG_AGO), minimal(FRI), done(FRI))));
    }

    @Test
    void entryOnAnotherDayDoesNotCount() {
        assertEquals(DayStatus.NOT_DONE, statusOn(tracker(checkDaily(LONG_AGO), done(THU))));
    }

    @Test
    void countBelowTargetIsNotDone() {
        assertEquals(DayStatus.NOT_DONE, statusOn(tracker(countDaily(8, LONG_AGO), amount(FRI, 7.5))));
    }

    @Test
    void countAtTargetIsDone() {
        assertEquals(DayStatus.DONE, statusOn(tracker(countDaily(8, LONG_AGO), amount(FRI, 8))));
    }

    @Test
    void countAboveTargetIsDone() {
        assertEquals(DayStatus.DONE, statusOn(tracker(countDaily(8, LONG_AGO), amount(FRI, 12))));
    }

    @Test
    void countSumsAllEntriesOfTheDay() {
        assertEquals(DayStatus.DONE, statusOn(tracker(countDaily(8, LONG_AGO), amount(FRI, 5), amount(FRI, 3))));
        assertEquals(DayStatus.NOT_DONE, statusOn(tracker(countDaily(8, LONG_AGO), amount(FRI, 5), amount(FRI, 2))));
    }

    @Test
    void countToleratesFloatingPointRounding() {
        // 0.1 + 0.7 daje w double 0.7999999999999999, czyli mniej niż 0.8
        assertEquals(DayStatus.DONE, statusOn(tracker(countDaily(0.8, LONG_AGO), amount(FRI, 0.1), amount(FRI, 0.7))));
    }

    @Test
    void countWithMinimalEntryBelowTargetIsDoneMinimal() {
        assertEquals(DayStatus.DONE_MINIMAL, statusOn(tracker(countDaily(8, LONG_AGO), minimalAmount(FRI, 2))));
    }

    @Test
    void countMinimalEntryWithZeroValueStillCountsAsMinimalDone() {
        assertEquals(DayStatus.DONE_MINIMAL, statusOn(tracker(countDaily(8, LONG_AGO), minimalAmount(FRI, 0))));
    }

    @Test
    void countReachingTargetIsFullyDoneEvenWithMinimalFlag() {
        assertEquals(DayStatus.DONE, statusOn(tracker(countDaily(8, LONG_AGO), minimalAmount(FRI, 8))));
    }

    @Test
    void entriesOfOtherHabitsAreIgnored() {
        assertEquals(DayStatus.NOT_DONE, statusOn(tracker(checkDaily(LONG_AGO), doneBy("inny", FRI))));
    }

    @Test
    void cellReportsTheSumOfValues() {
        HabitTracker tracker = tracker(countDaily(8, LONG_AGO), amount(FRI, 2.5), amount(FRI, 1));

        assertEquals(3.5, tracker.day(FRI, TODAY).amount());
        assertEquals(0.0, tracker.day(THU, TODAY).amount());
    }

    @Test
    void cellFlagsFutureDays() {
        HabitTracker tracker = tracker(checkDaily(LONG_AGO));

        assertFalse(tracker.day(TODAY, TODAY).future());
        assertTrue(tracker.day(SUN, TODAY).future());
        assertFalse(tracker.day(SUN, TODAY).editable());
        assertTrue(tracker.day(TODAY, TODAY).editable());
    }

    @Test
    void cellFlagsDaysBeforeCreation() {
        HabitTracker tracker = tracker(checkDaily(WED));

        assertTrue(tracker.day(WED.minusDays(1), TODAY).beforeCreated());
        assertFalse(tracker.day(WED.minusDays(1), TODAY).editable());
        assertFalse(tracker.day(WED, TODAY).beforeCreated());
        assertTrue(tracker.day(WED, TODAY).editable());
    }

    @Test
    void pastDaysCanBeBackfilled() {
        assertTrue(tracker(checkDaily(LONG_AGO)).day(Fixtures.PREV_MON, TODAY).editable());
    }

    @Test
    void pausedDayKeepsItsEntryStatusButIsFlagged() {
        HabitTracker tracker = tracker(checkDaily(LONG_AGO), pauses(pause(THU, FRI)), done(THU));

        assertTrue(tracker.day(THU, TODAY).paused());
        assertEquals(DayStatus.DONE, tracker.day(THU, TODAY).status());
        assertTrue(tracker.day(FRI, TODAY).paused());
        assertEquals(DayStatus.NOT_DONE, tracker.day(FRI, TODAY).status());
        assertFalse(tracker.day(SAT, TODAY).paused());
    }
}
