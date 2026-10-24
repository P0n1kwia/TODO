package pl.mojrytm.model;

import java.time.LocalDate;

/** Zadanie. Limit trzech zadań w statusie TODAY pilnuje warstwa service. */
public record Task(String id, String title, TaskStatus status, LocalDate dueDate, LocalDate doneDate) {

    public Task {
        Checks.requireText(id, "id");
        Checks.requireText(title, "title");
        Checks.requireNonNull(status, "status");
    }
}
