package pl.mojrytm.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import pl.mojrytm.model.AppData;

class MigratorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static ObjectNode fileWithVersion(int version) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("schemaVersion", version);
        return root;
    }

    private static Migration step(int from, Consumer<ObjectNode> action) {
        return new Migration() {
            @Override
            public int fromVersion() {
                return from;
            }

            @Override
            public void migrate(ObjectNode root) {
                action.accept(root);
            }
        };
    }

    @Test
    void fileInCurrentVersionIsUntouched() throws Exception {
        List<Integer> called = new ArrayList<>();
        Migrator migrator = new Migrator(2, List.of(step(1, r -> called.add(1))));
        ObjectNode root = fileWithVersion(2);

        migrator.migrate(root);

        assertEquals(2, root.get("schemaVersion").intValue());
        assertEquals(List.of(), called);
    }

    @Test
    void appliesMigrationsOneVersionAtATimeInOrder() throws Exception {
        List<Integer> called = new ArrayList<>();
        Migrator migrator = new Migrator(3, List.of(
                step(2, r -> {
                    called.add(2);
                    assertEquals(2, r.get("schemaVersion").intValue());
                    r.put("addedIn2", true);
                }),
                step(1, r -> {
                    called.add(1);
                    assertEquals(1, r.get("schemaVersion").intValue());
                    r.put("addedIn1", true);
                })));
        ObjectNode root = fileWithVersion(1);

        migrator.migrate(root);

        assertEquals(List.of(1, 2), called);
        assertEquals(3, root.get("schemaVersion").intValue());
        assertTrue(root.get("addedIn1").booleanValue());
        assertTrue(root.get("addedIn2").booleanValue());
    }

    @Test
    void startsFromTheVersionOfTheFile() throws Exception {
        List<Integer> called = new ArrayList<>();
        Migrator migrator = new Migrator(3, List.of(step(1, r -> called.add(1)), step(2, r -> called.add(2))));

        migrator.migrate(fileWithVersion(2));

        assertEquals(List.of(2), called);
    }

    @Test
    void newerFileIsRejectedAndLeftUntouched() {
        Migrator migrator = new Migrator(2, List.of());
        ObjectNode root = fileWithVersion(5);

        NewerSchemaVersionException e = assertThrows(NewerSchemaVersionException.class, () -> migrator.migrate(root));

        assertEquals(5, e.fileVersion());
        assertEquals(2, e.appVersion());
        assertEquals(5, root.get("schemaVersion").intValue());
    }

    @Test
    void missingMigrationStepIsAnError() {
        Migrator migrator = new Migrator(3, List.of(step(2, r -> { })));

        assertThrows(MigrationException.class, () -> migrator.migrate(fileWithVersion(1)));
    }

    @Test
    void failingMigrationIsReportedAsMigrationException() {
        Migrator migrator = new Migrator(2, List.of(step(1, r -> {
            throw new IllegalStateException("zepsute");
        })));

        MigrationException e = assertThrows(MigrationException.class, () -> migrator.migrate(fileWithVersion(1)));

        assertInstanceOf(IllegalStateException.class, e.getCause());
    }

    @Test
    void missingOrNonIntegerVersionIsAnError() {
        Migrator migrator = new Migrator(1, List.of());
        ObjectNode noVersion = MAPPER.createObjectNode();
        ObjectNode textVersion = MAPPER.createObjectNode().put("schemaVersion", "1");
        ObjectNode fractionalVersion = MAPPER.createObjectNode().put("schemaVersion", 1.5);

        assertThrows(DataFormatException.class, () -> migrator.migrate(noVersion));
        assertThrows(DataFormatException.class, () -> migrator.migrate(textVersion));
        assertThrows(DataFormatException.class, () -> migrator.migrate(fractionalVersion));
    }

    @Test
    void versionBelowFirstWithoutMigrationIsAnError() {
        Migrator migrator = new Migrator(1, List.of());

        assertThrows(MigrationException.class, () -> migrator.migrate(fileWithVersion(0)));
    }

    @Test
    void twoMigrationsFromTheSameVersionAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Migrator(2, List.of(step(1, r -> { }), step(1, r -> { }))));
    }

    @Test
    void canMigrateFromReflectsTheChain() {
        Migrator migrator = new Migrator(3, List.of(step(1, r -> { }), step(2, r -> { })));

        assertTrue(migrator.canMigrateFrom(1));
        assertTrue(migrator.canMigrateFrom(3));
        assertFalse(migrator.canMigrateFrom(4));
        assertFalse(new Migrator(3, List.of(step(2, r -> { }))).canMigrateFrom(1));
    }

    /** Strażnik: po podbiciu schematu bez dopisania migracji ten test przestanie przechodzić. */
    @Test
    void standardMigratorCoversEveryOlderVersion() {
        Migrator standard = Migrator.standard();

        for (int version = 1; version <= AppData.CURRENT_SCHEMA_VERSION; version++) {
            assertTrue(standard.canMigrateFrom(version), "brak migracji z wersji " + version);
        }
    }
}
