package pl.mojrytm.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Area;
import pl.mojrytm.model.WheelRating;

/**
 * Zestawienia koła życia dla widoku. Pokazujemy tylko aktywne obszary w kolejności z danych; oceny
 * zarchiwizowanych obszarów zostają w pliku, ale nie trafiają do widoku. Gdyby w pliku były dwie oceny
 * tego samego obszaru z tego samego dnia (np. po ręcznej edycji), liczy się późniejsza w danych.
 */
public final class WheelBoard {

    /** Aktywne obszary z ostatnią i poprzednią oceną. */
    public List<AreaRating> areas(AppData data) {
        Map<String, List<WheelRating>> byArea = ratingsByArea(data);
        return activeAreas(data).stream().map(area -> {
            List<WheelRating> ratings = byArea.getOrDefault(area.id(), List.of());
            Optional<WheelRating> last = ratings.isEmpty() ? Optional.empty()
                    : Optional.of(ratings.get(ratings.size() - 1));
            Optional<WheelRating> previous = ratings.size() < 2 ? Optional.empty()
                    : Optional.of(ratings.get(ratings.size() - 2));
            return new AreaRating(area, last, previous);
        }).toList();
    }

    /** Serie do wykresu: aktywne obszary, które mają przynajmniej jedną ocenę. */
    public List<AreaSeries> series(AppData data) {
        Map<String, List<WheelRating>> byArea = ratingsByArea(data);
        return activeAreas(data).stream()
                .filter(area -> byArea.containsKey(area.id()))
                .map(area -> new AreaSeries(area, byArea.get(area.id())))
                .toList();
    }

    /** Dni, w których oceniono przynajmniej jeden aktywny obszar, od najstarszego. */
    public List<LocalDate> ratedDays(AppData data) {
        return series(data).stream()
                .flatMap(series -> series.points().stream())
                .map(WheelRating::date)
                .distinct()
                .sorted()
                .toList();
    }

    private static List<Area> activeAreas(AppData data) {
        return data.areas().stream().filter(area -> !area.archived()).toList();
    }

    /** Oceny każdego obszaru od najstarszej, po jednej na dzień (przy powtórzeniu wygrywa późniejsza w danych). */
    private static Map<String, List<WheelRating>> ratingsByArea(AppData data) {
        Map<String, Map<LocalDate, WheelRating>> latestPerDay = new LinkedHashMap<>();
        for (WheelRating rating : data.wheelRatings()) {
            latestPerDay.computeIfAbsent(rating.areaId(), id -> new LinkedHashMap<>()).put(rating.date(), rating);
        }
        return latestPerDay.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<WheelRating> sorted = new ArrayList<>(entry.getValue().values());
                    sorted.sort(Comparator.comparing(WheelRating::date));
                    return List.copyOf(sorted);
                }));
    }
}
