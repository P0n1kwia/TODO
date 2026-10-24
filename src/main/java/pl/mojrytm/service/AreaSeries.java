package pl.mojrytm.service;

import java.util.List;
import pl.mojrytm.model.Area;
import pl.mojrytm.model.WheelRating;

/** Oceny jednego obszaru w czasie, od najstarszej, jedna na dzień. */
public record AreaSeries(Area area, List<WheelRating> points) {
}
