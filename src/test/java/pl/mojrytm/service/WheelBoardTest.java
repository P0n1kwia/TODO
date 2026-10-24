package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Area;
import pl.mojrytm.model.WheelRating;

class WheelBoardTest {

    private static final LocalDate D1 = LocalDate.of(2026, 9, 1);
    private static final LocalDate D2 = LocalDate.of(2026, 10, 1);
    private static final LocalDate D3 = LocalDate.of(2026, 10, 24);

    private final WheelBoard board = new WheelBoard();

    private static AppData data(WheelRating... ratings) {
        return AppData.empty()
                .withAreas(List.of(new Area("old", "Stary", true), new Area("sen", "Sen", false),
                        new Area("cialo", "Ciało", false)))
                .withWheelRatings(List.of(ratings));
    }

    private static WheelRating rating(LocalDate date, String areaId, int score) {
        return new WheelRating(date, areaId, score);
    }

    @Test
    void areasAreTheActiveOnesInDataOrder() {
        List<AreaRating> areas = board.areas(data());

        assertEquals(List.of("Sen", "Ciało"), areas.stream().map(row -> row.area().name()).toList());
    }

    @Test
    void lastPreviousAndChangeComeFromTheTwoNewestDays() {
        AppData data = data(rating(D3, "sen", 8), rating(D1, "sen", 3), rating(D2, "sen", 5));

        AreaRating sen = board.areas(data).get(0);

        assertEquals(Optional.of(rating(D3, "sen", 8)), sen.last());
        assertEquals(Optional.of(rating(D2, "sen", 5)), sen.previous());
        assertEquals(OptionalInt.of(3), sen.change());
    }

    @Test
    void aDropGivesANegativeChangeAndNoChangeGivesZero() {
        AppData data = data(rating(D1, "sen", 8), rating(D2, "sen", 5), rating(D1, "cialo", 6), rating(D2, "cialo", 6));

        List<AreaRating> areas = board.areas(data);

        assertEquals(OptionalInt.of(-3), areas.get(0).change());
        assertEquals(OptionalInt.of(0), areas.get(1).change());
    }

    @Test
    void aSingleRatingHasNoPreviousAndNoChange() {
        AreaRating sen = board.areas(data(rating(D2, "sen", 6))).get(0);

        assertEquals(Optional.of(rating(D2, "sen", 6)), sen.last());
        assertEquals(Optional.empty(), sen.previous());
        assertEquals(OptionalInt.empty(), sen.change());
    }

    @Test
    void anAreaNeverRatedHasNothing() {
        AreaRating cialo = board.areas(data(rating(D2, "sen", 6))).get(1);

        assertEquals(Optional.empty(), cialo.last());
        assertEquals(OptionalInt.empty(), cialo.change());
    }

    @Test
    void theSuggestedScoreIsTheLastOneOrFive() {
        List<AreaRating> areas = board.areas(data(rating(D1, "sen", 3), rating(D2, "sen", 9)));

        assertEquals(9, areas.get(0).suggestedScore());
        assertEquals(5, areas.get(1).suggestedScore());
    }

    @Test
    void twoRatingsOfOneDayCountOnceAndTheLaterInTheDataWins() {
        AppData data = data(rating(D2, "sen", 4), rating(D1, "sen", 2), rating(D2, "sen", 7));

        AreaRating sen = board.areas(data).get(0);

        assertEquals(Optional.of(rating(D2, "sen", 7)), sen.last());
        assertEquals(Optional.of(rating(D1, "sen", 2)), sen.previous());
        assertEquals(List.of(rating(D1, "sen", 2), rating(D2, "sen", 7)), board.series(data).get(0).points());
    }

    @Test
    void seriesCoverOnlyActiveAreasWithRatingsSortedByDate() {
        AppData data = data(rating(D3, "sen", 8), rating(D1, "old", 5), rating(D1, "sen", 3));

        List<AreaSeries> series = board.series(data);

        assertEquals(1, series.size());
        assertEquals("Sen", series.get(0).area().name());
        assertEquals(List.of(D1, D3), series.get(0).points().stream().map(WheelRating::date).toList());
    }

    @Test
    void ratedDaysAreTheDistinctDaysOfActiveAreasOldestFirst() {
        AppData data = data(rating(D3, "sen", 8), rating(D2, "cialo", 5), rating(D3, "cialo", 6),
                rating(D1, "old", 9));

        assertEquals(List.of(D2, D3), board.ratedDays(data));
    }

    @Test
    void withoutRatingsEverythingIsEmpty() {
        assertEquals(List.of(), board.series(data()));
        assertEquals(List.of(), board.ratedDays(data()));
        assertTrue(board.areas(AppData.empty()).isEmpty());
    }
}
