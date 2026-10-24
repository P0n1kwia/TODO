package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.WheelRating;
import pl.mojrytm.storage.JsonStorage;

/** Obszary z UiTestData.sampleWithGoals(): Sen i Ciało (aktywne) oraz Stary (zarchiwizowany). Dziś 2026-10-24. */
class WheelScreenTest {

    private static final LocalDate TODAY = UiTestData.TODAY;
    private static final LocalDate EARLY = LocalDate.of(2026, 10, 1);
    private static final LocalDate LATER = LocalDate.of(2026, 10, 17);

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock("2026-10-24T10:00:00Z");
    private AppModel model;
    private WheelScreen screen;

    @BeforeAll
    static void toolkit() {
        FxTestSupport.assumeToolkit();
    }

    @AfterEach
    void tearDown() throws IOException {
        model.shutdown();
    }

    private void open(AppData data) {
        model = new AppModel(data, new JsonStorage(dir, clock), clock);
        screen = FxTestSupport.onFx(() -> new WheelScreen(model));
    }

    private static AppData withRatings(WheelRating... ratings) {
        return UiTestData.sampleWithGoals().withWheelRatings(List.of(ratings));
    }

    private static WheelRating rating(LocalDate date, String areaId, int score) {
        return new WheelRating(date, areaId, score);
    }

    private Slider slider(String areaId) {
        return (Slider) screen.lookup("#wheel-slider-" + areaId);
    }

    private void setSlider(String areaId, double value) {
        FxTestSupport.onFx(() -> slider(areaId).setValue(value));
    }

    private String label(String id) {
        return ((Label) screen.lookup("#" + id)).getText();
    }

    private void save() {
        FxTestSupport.onFx(((Button) screen.lookup("#wheel-save"))::fire);
    }

    @SuppressWarnings("unchecked")
    private LineChart<String, Number> chart() {
        return (LineChart<String, Number>) screen.lookup("#wheel-chart");
    }

    private static List<String> points(XYChart.Series<String, Number> series) {
        return series.getData().stream().map(d -> d.getXValue() + "=" + d.getYValue()).toList();
    }

    @Test
    void withoutActiveAreasPointsToTheGoalsTab() {
        open(AppData.empty());

        assertEquals(List.of("Najpierw dodaj obszary w zakładce Cele."),
                screen.lookupAll(".wheel-empty").stream().map(n -> ((Label) n).getText()).toList());
        assertNull(screen.lookup("#wheel-save"));
        assertNull(chart());
    }

    @Test
    void anAreaNeverRatedStartsAtFiveAndSaysSo() {
        open(withRatings());

        assertEquals(5, slider("sen").getValue());
        assertEquals("5", label("wheel-value-sen"));
        assertEquals("Jeszcze nie oceniano", label("wheel-meta-sen"));
        assertNull(chart());
        assertEquals(1, screen.lookupAll(".wheel-chart-empty").size());
    }

    @Test
    void theScaleIsOneToTenAndOnlyActiveAreasGetASlider() {
        open(withRatings());

        assertEquals(1, slider("sen").getMin());
        assertEquals(10, slider("sen").getMax());
        assertNull(slider("old"));
        assertTrue(slider("cialo") != null);
    }

    @Test
    void movingASliderUpdatesItsValueLabel() {
        open(withRatings());

        setSlider("sen", 8);

        assertEquals("8", label("wheel-value-sen"));
    }

    @Test
    void savingStoresOneRatingPerActiveAreaForTodayAndDrawsTheChart() {
        open(withRatings());
        setSlider("sen", 8);
        setSlider("cialo", 3);

        save();

        assertEquals(List.of(rating(TODAY, "sen", 8), rating(TODAY, "cialo", 3)), model.data().wheelRatings());
        assertEquals("Zapisano ocenę koła życia z dnia 24.10.2026", model.statusProperty().get().text());
        assertEquals(2, chart().getData().size());
        assertEquals("Ostatnio: 8 z 24.10.2026", label("wheel-meta-sen"));
        assertEquals(0, screen.lookupAll(".wheel-chart-empty").size());
    }

    @Test
    void aSliderBetweenTicksIsRoundedOnSave() {
        open(withRatings());
        setSlider("sen", 7.4);
        assertEquals("7", label("wheel-value-sen"));

        save();

        assertEquals(7, model.data().wheelRatings().get(0).score());
    }

    @Test
    void slidersStartAtTheLastRatingAndShowItWithTheChange() {
        open(withRatings(rating(EARLY, "sen", 4), rating(LATER, "sen", 6), rating(LATER, "cialo", 7)));

        assertEquals(6, slider("sen").getValue());
        assertEquals(7, slider("cialo").getValue());
        assertEquals("Ostatnio: 6 z 17.10.2026 · zmiana +2", label("wheel-meta-sen"));
        assertEquals("Ostatnio: 7 z 17.10.2026", label("wheel-meta-cialo"));
    }

    @Test
    void aDropIsShownWithATrueMinusSign() {
        open(withRatings(rating(EARLY, "sen", 8), rating(LATER, "sen", 5)));

        assertEquals("Ostatnio: 5 z 17.10.2026 · zmiana −3", label("wheel-meta-sen"));
    }

    @Test
    void theChartHasALinePerActiveAreaWithItsRatingsInTime() {
        open(withRatings(rating(EARLY, "sen", 4), rating(LATER, "sen", 6), rating(LATER, "cialo", 7),
                rating(EARLY, "old", 9)));

        List<XYChart.Series<String, Number>> data = chart().getData();

        assertEquals(List.of("Sen", "Ciało"), data.stream().map(XYChart.Series::getName).toList());
        assertEquals(List.of("01.10.2026=4", "17.10.2026=6"), points(data.get(0)));
        assertEquals(List.of("17.10.2026=7"), points(data.get(1)));
    }

    @Test
    void ratingAgainTheSameDayOverwritesAndKeepsOnePointPerDay() {
        open(withRatings());
        setSlider("sen", 4);
        save();
        setSlider("sen", 9);

        save();

        assertEquals(2, model.data().wheelRatings().size());
        assertEquals(9, model.data().wheelRatings().get(0).score());
        assertEquals(List.of("24.10.2026=9"), points(chart().getData().get(0)));
    }

    @Test
    void oldRatingsStayWhenANewOneIsSaved() {
        open(withRatings(rating(EARLY, "sen", 4)));
        setSlider("sen", 6);

        save();

        assertEquals(List.of(rating(EARLY, "sen", 4), rating(TODAY, "sen", 6), rating(TODAY, "cialo", 5)),
                model.data().wheelRatings());
        assertEquals(List.of("01.10.2026=4", "24.10.2026=6"), points(chart().getData().get(0)));
    }

    @Test
    void archivingAnAreaRemovesItsSliderButKeepsItsRatings() {
        open(withRatings(rating(EARLY, "sen", 4), rating(EARLY, "cialo", 6)));

        FxTestSupport.onFx(() -> model.update(data -> model.areas().setArchived(data, "cialo", true)));

        assertNull(slider("cialo"));
        assertEquals(List.of("Sen"), chart().getData().stream().map(XYChart.Series::getName).toList());
        assertEquals(2, model.data().wheelRatings().size());
    }
}
