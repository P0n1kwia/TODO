package pl.mojrytm.model;

/** Cel w obszarze. Nie usuwamy, tylko archiwizujemy. */
public record Goal(String id, String areaId, String name, boolean archived) {

    public Goal {
        Checks.requireText(id, "id");
        Checks.requireText(areaId, "areaId");
        Checks.requireText(name, "name");
    }
}
