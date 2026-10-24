package pl.mojrytm.model;

import java.time.LocalDate;

/** Ocena obszaru w kole życia (1–10) w danym dniu. */
public record WheelRating(LocalDate date, String areaId, int score) {

    public WheelRating {
        Checks.requireNonNull(date, "date");
        Checks.requireText(areaId, "areaId");
        Checks.requireRange(score, 1, 10, "score");
    }
}
