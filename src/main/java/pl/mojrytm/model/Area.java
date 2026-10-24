package pl.mojrytm.model;

/** Obszar życia (np. Sen, Ciało, Pieniądze, Relacje). */
public record Area(String id, String name) {

    public Area {
        Checks.requireText(id, "id");
        Checks.requireText(name, "name");
    }
}
