package pl.mojrytm.service;

import pl.mojrytm.model.Goal;

/** Cel i liczba jego aktywnych (niezarchiwizowanych) nawyków. */
public record GoalNode(Goal goal, int activeHabits) {
}
