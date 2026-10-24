package pl.mojrytm.service;

/** Seria kolejnych zaliczonych dni (nawyk dzienny) albo tygodni (nawyk tygodniowy). */
public record Streak(int length, StreakUnit unit) {
}
