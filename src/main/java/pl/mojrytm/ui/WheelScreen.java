package pl.mojrytm.ui;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pl.mojrytm.model.WheelRating;
import pl.mojrytm.service.AreaRating;
import pl.mojrytm.service.AreaSeries;

/**
 * Ekran „Koło życia": oceny 1–10 aktywnych obszarów (suwaki, zapis na dziś) i wykres zmiany ocen w czasie.
 * JavaFX nie ma wykresu radarowego, więc zmianę pokazuje wbudowany wykres liniowy: jedna linia na obszar.
 */
final class WheelScreen extends VBox {

    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 10;

    private final AppModel model;

    WheelScreen(AppModel model) {
        super(12);
        this.model = model;
        getStyleClass().add("screen-content");
        model.dataProperty().addListener((observable, previous, current) -> rebuild());
        rebuild();
    }

    private void rebuild() {
        getChildren().clear();
        Label title = new Label(Messages.get("wheel.title"));
        title.getStyleClass().add("screen-title");
        Label intro = new Label(Messages.get("wheel.intro"));
        intro.getStyleClass().add("habit-meta");
        intro.setWrapText(true);
        getChildren().addAll(title, intro);

        List<AreaRating> areas = model.wheelBoard().areas(model.data());
        if (areas.isEmpty()) {
            Label empty = new Label(Messages.get("wheel.noAreas"));
            empty.getStyleClass().addAll("habit-meta", "wheel-empty");
            getChildren().add(empty);
            return;
        }
        getChildren().addAll(ratingPanel(areas), chartPanel());
    }

    private Node ratingPanel(List<AreaRating> areas) {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("wheel-panel");
        Map<String, Slider> sliders = new LinkedHashMap<>();
        for (AreaRating row : areas) {
            Slider slider = slider(row);
            sliders.put(row.area().id(), slider);
            panel.getChildren().add(ratingRow(row, slider));
        }
        Button save = new Button(Messages.get("wheel.save"));
        save.setId("wheel-save");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> {
            Map<String, Integer> scores = new LinkedHashMap<>();
            sliders.forEach((areaId, slider) -> scores.put(areaId, (int) Math.round(slider.getValue())));
            LocalDate day = model.today();
            if (model.update(data -> model.wheel().rate(data, day, scores))) {
                model.showInfo(Messages.format("wheel.saved.info", Formats.numericDate(day)));
            }
        });
        panel.getChildren().add(save);
        return panel;
    }

    private static Slider slider(AreaRating row) {
        Slider slider = new Slider(MIN_SCORE, MAX_SCORE, row.suggestedScore());
        slider.setId("wheel-slider-" + row.area().id());
        slider.setMajorTickUnit(1);
        slider.setMinorTickCount(0);
        slider.setSnapToTicks(true);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setBlockIncrement(1);
        slider.setPrefWidth(380);
        return slider;
    }

    private static Node ratingRow(AreaRating row, Slider slider) {
        Label name = new Label(row.area().name());
        name.getStyleClass().add("habit-name");
        name.setMinWidth(150);
        Label value = new Label(String.valueOf((int) Math.round(slider.getValue())));
        value.setId("wheel-value-" + row.area().id());
        value.getStyleClass().add("wheel-value");
        slider.valueProperty().addListener((observable, previous, current) ->
                value.setText(String.valueOf((int) Math.round(current.doubleValue()))));
        Label meta = new Label(lastRatingText(row));
        meta.setId("wheel-meta-" + row.area().id());
        meta.getStyleClass().add("habit-meta");
        HBox.setHgrow(meta, Priority.ALWAYS);

        HBox line = new HBox(14, name, slider, value, meta);
        line.setAlignment(Pos.CENTER_LEFT);
        return line;
    }

    /** "Ostatnio: 6 z 01.10.2026 · zmiana +2" albo "Jeszcze nie oceniano". */
    private static String lastRatingText(AreaRating row) {
        if (row.last().isEmpty()) {
            return Messages.get("wheel.never");
        }
        WheelRating last = row.last().get();
        String text = Messages.format("wheel.last", last.score(), Formats.numericDate(last.date()));
        OptionalInt change = row.change();
        return change.isPresent()
                ? text + " · " + Messages.format("wheel.change", Formats.change(change.getAsInt()))
                : text;
    }

    private Node chartPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("wheel-panel");
        Label heading = new Label(Messages.get("wheel.chart.title"));
        heading.getStyleClass().add("section-title");
        panel.getChildren().add(heading);

        List<AreaSeries> series = model.wheelBoard().series(model.data());
        if (series.isEmpty()) {
            Label empty = new Label(Messages.get("wheel.chart.empty"));
            empty.getStyleClass().addAll("habit-meta", "wheel-chart-empty");
            empty.setWrapText(true);
            panel.getChildren().add(empty);
            return panel;
        }

        List<String> days = model.wheelBoard().ratedDays(model.data()).stream().map(Formats::numericDate).toList();
        CategoryAxis xAxis = new CategoryAxis(FXCollections.observableArrayList(days));
        xAxis.setLabel(Messages.get("wheel.chart.x"));
        NumberAxis yAxis = new NumberAxis(0, MAX_SCORE, 1);
        yAxis.setAutoRanging(false);
        yAxis.setMinorTickVisible(false);
        yAxis.setLabel(Messages.get("wheel.chart.y"));

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setId("wheel-chart");
        chart.setAnimated(false);
        chart.setLegendSide(Side.BOTTOM);
        chart.setPrefHeight(380);
        for (AreaSeries areaSeries : series) {
            XYChart.Series<String, Number> line = new XYChart.Series<>();
            line.setName(areaSeries.area().name());
            for (WheelRating rating : areaSeries.points()) {
                line.getData().add(new XYChart.Data<>(Formats.numericDate(rating.date()), rating.score()));
            }
            chart.getData().add(line);
        }
        panel.getChildren().add(chart);
        return panel;
    }
}
