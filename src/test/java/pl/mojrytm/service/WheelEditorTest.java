package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Area;
import pl.mojrytm.model.WheelRating;

class WheelEditorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);
    private static final LocalDate EARLIER = LocalDate.of(2026, 10, 1);

    private final WheelEditor editor = new WheelEditor();

    private static AppData withAreas() {
        return AppData.empty().withAreas(List.of(new Area("sen", "Sen", false), new Area("cialo", "Ciało", false),
                new Area("old", "Stary", true)));
    }

    private static Map<String, Integer> scores(Object... pairs) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return map;
    }

    @Test
    void ratingsAreAddedForTheGivenDayInTheGivenOrder() {
        AppData data = editor.rate(withAreas(), TODAY, scores("cialo", 7, "sen", 4));

        assertEquals(List.of(new WheelRating(TODAY, "cialo", 7), new WheelRating(TODAY, "sen", 4)),
                data.wheelRatings());
    }

    @Test
    void ratingAgainTheSameDayOverwritesInPlace() {
        AppData first = editor.rate(withAreas(), TODAY, scores("sen", 4, "cialo", 7));

        AppData second = editor.rate(first, TODAY, scores("sen", 9));

        assertEquals(List.of(new WheelRating(TODAY, "sen", 9), new WheelRating(TODAY, "cialo", 7)),
                second.wheelRatings());
    }

    @Test
    void olderRatingsAreNotTouched() {
        AppData before = withAreas().withWheelRatings(List.of(new WheelRating(EARLIER, "sen", 3)));

        AppData after = editor.rate(before, TODAY, scores("sen", 8));

        assertEquals(List.of(new WheelRating(EARLIER, "sen", 3), new WheelRating(TODAY, "sen", 8)),
                after.wheelRatings());
    }

    @Test
    void savingTheSameScoresAgainChangesNothing() {
        AppData data = editor.rate(withAreas(), TODAY, scores("sen", 4));

        assertSame(data, editor.rate(data, TODAY, scores("sen", 4)));
    }

    @Test
    void nothingToRateChangesNothing() {
        AppData data = withAreas();

        assertSame(data, editor.rate(data, TODAY, Map.of()));
    }

    @Test
    void unknownAndArchivedAreasAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> editor.rate(withAreas(), TODAY, scores("nie-ma", 5)));
        assertThrows(IllegalArgumentException.class, () -> editor.rate(withAreas(), TODAY, scores("old", 5)));
    }

    @Test
    void scoresOutsideOneToTenAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> editor.rate(withAreas(), TODAY, scores("sen", 0)));
        assertThrows(IllegalArgumentException.class, () -> editor.rate(withAreas(), TODAY, scores("sen", 11)));
        assertEquals(1, editor.rate(withAreas(), TODAY, scores("sen", 1)).wheelRatings().size());
        assertEquals(1, editor.rate(withAreas(), TODAY, scores("sen", 10)).wheelRatings().size());
    }

    @Test
    void aRejectedBatchLeavesTheDataUntouched() {
        AppData data = withAreas();

        assertThrows(IllegalArgumentException.class, () -> editor.rate(data, TODAY, scores("sen", 5, "old", 5)));

        assertEquals(List.of(), data.wheelRatings());
    }
}
