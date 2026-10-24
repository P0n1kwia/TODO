package pl.mojrytm.service;

/**
 * Cel opisany z nazwą obszaru, do wyboru w oknie nawyku i do pokazania przy nawyku.
 * {@code archived} jest prawdą, gdy zarchiwizowany jest cel albo jego obszar.
 */
public record GoalOption(String goalId, String areaName, String goalName, boolean archived) {
}
