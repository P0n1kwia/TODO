package pl.mojrytm.service;

import java.util.List;
import pl.mojrytm.model.Area;

/** Obszar z jego celami (aktywne najpierw, potem zarchiwizowane). */
public record AreaNode(Area area, List<GoalNode> goals) {
}
