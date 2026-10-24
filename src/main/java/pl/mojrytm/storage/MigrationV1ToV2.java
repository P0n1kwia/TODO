package pl.mojrytm.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Wersja 2: obszary dostają pole {@code archived} (dotychczasowe są aktywne). */
final class MigrationV1ToV2 implements Migration {

    @Override
    public int fromVersion() {
        return 1;
    }

    @Override
    public void migrate(ObjectNode root) {
        JsonNode areas = root.get("areas");
        if (areas == null || !areas.isArray()) {
            return; // brak obszarów albo dziwny kształt: zostawiamy, resztą zajmie się walidacja modelu
        }
        for (JsonNode area : areas) {
            if (area instanceof ObjectNode object && !object.has("archived")) {
                object.put("archived", false);
            }
        }
    }
}
