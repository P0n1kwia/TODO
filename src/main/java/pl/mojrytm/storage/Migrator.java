package pl.mojrytm.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pl.mojrytm.model.AppData;

/**
 * Łańcuch migracji schematu, wersja po wersji. Działa na drzewie JSON, przed zamianą na model,
 * więc stare pliki nie muszą pasować do aktualnych klas.
 */
public final class Migrator {

    static final String VERSION_FIELD = "schemaVersion";

    private final int targetVersion;
    private final Map<Integer, Migration> byVersion = new HashMap<>();

    public Migrator(int targetVersion, List<Migration> migrations) {
        this.targetVersion = targetVersion;
        for (Migration migration : migrations) {
            if (byVersion.put(migration.fromVersion(), migration) != null) {
                throw new IllegalArgumentException("Dwie migracje z wersji " + migration.fromVersion());
            }
        }
    }

    /** Migracje aplikacji. Przy każdej zmianie modelu: podbij AppData.CURRENT_SCHEMA_VERSION i dopisz tu migrację. */
    public static Migrator standard() {
        return new Migrator(AppData.CURRENT_SCHEMA_VERSION, List.of(new MigrationV1ToV2()));
    }

    /** Czy istnieje komplet migracji z podanej wersji do bieżącej. */
    public boolean canMigrateFrom(int version) {
        if (version > targetVersion) {
            return false;
        }
        for (int v = version; v < targetVersion; v++) {
            if (!byVersion.containsKey(v)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Przenosi drzewo na bieżącą wersję (zmienia je w miejscu).
     *
     * @throws NewerSchemaVersionException plik jest nowszy niż aplikacja
     * @throws DataFormatException         brak lub błędny numer wersji, brak migracji albo migracja się nie powiodła
     */
    public ObjectNode migrate(ObjectNode root) throws DataFormatException {
        JsonNode versionNode = root.get(VERSION_FIELD);
        if (versionNode == null || !versionNode.isInt()) {
            throw new DataFormatException("Brak numeru wersji schematu (" + VERSION_FIELD + ")");
        }
        int version = versionNode.intValue();
        if (version > targetVersion) {
            throw new NewerSchemaVersionException(version, targetVersion);
        }
        while (version < targetVersion) {
            Migration migration = byVersion.get(version);
            if (migration == null) {
                throw new MigrationException("Brak migracji z wersji " + version + " na " + (version + 1));
            }
            try {
                migration.migrate(root);
            } catch (RuntimeException e) {
                throw new MigrationException("Migracja z wersji " + version + " nie powiodła się: " + e.getMessage(), e);
            }
            version++;
            root.put(VERSION_FIELD, version);
        }
        return root;
    }
}
