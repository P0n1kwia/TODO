package pl.mojrytm.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.WheelRating;

/**
 * Zapis ocen koła życia. Na obszar przypada jedna ocena na dzień: zapis tego samego dnia nadpisuje
 * poprzednią (zostaje na swoim miejscu w danych), starsze oceny nie są ruszane. Oceniać można tylko
 * aktywne obszary i tylko dziś. Naruszenia to błąd programisty ({@link IllegalArgumentException}),
 * w tym ocena poza skalą 1–10 (pilnuje jej model).
 */
public final class WheelEditor {

    /**
     * Zapisuje oceny z dnia {@code today}.
     *
     * @param scoresByAreaId ocena 1–10 dla każdego obszaru; kolejność mapy to kolejność nowych wpisów
     */
    public AppData rate(AppData data, LocalDate today, Map<String, Integer> scoresByAreaId) {
        List<WheelRating> ratings = new ArrayList<>(data.wheelRatings());
        for (Map.Entry<String, Integer> entry : scoresByAreaId.entrySet()) {
            String areaId = entry.getKey();
            requireActiveArea(data, areaId);
            WheelRating rating = new WheelRating(today, areaId, entry.getValue());
            int index = indexOf(ratings, today, areaId);
            if (index < 0) {
                ratings.add(rating);
            } else {
                ratings.set(index, rating);
            }
        }
        return ratings.equals(data.wheelRatings()) ? data : data.withWheelRatings(ratings);
    }

    private static void requireActiveArea(AppData data, String areaId) {
        boolean active = data.areas().stream().anyMatch(area -> area.id().equals(areaId) && !area.archived());
        if (!active) {
            throw new IllegalArgumentException("Nie ma aktywnego obszaru o id: " + areaId);
        }
    }

    private static int indexOf(List<WheelRating> ratings, LocalDate date, String areaId) {
        for (int i = 0; i < ratings.size(); i++) {
            if (ratings.get(i).date().equals(date) && ratings.get(i).areaId().equals(areaId)) {
                return i;
            }
        }
        return -1;
    }
}
