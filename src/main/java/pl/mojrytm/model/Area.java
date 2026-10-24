package pl.mojrytm.model;

/** Obszar życia (np. Sen, Ciało, Pieniądze, Relacje). Nie usuwamy go, tylko archiwizujemy. */
public record Area(String id, String name, boolean archived) {

    public Area {
        Checks.requireText(id, "id");
        Checks.requireText(name, "name");
    }

    public Area withArchived(boolean newArchived) {
        return new Area(id, name, newArchived);
    }
}
