package pl.mojrytm.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.mojrytm.TestData;
import pl.mojrytm.model.AppData;

class MigrationV1ToV2Test {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-24T10:30:00Z"), ZoneOffset.UTC);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path dir;

    private static byte[] sampleV1() throws IOException {
        try (InputStream in = MigrationV1ToV2Test.class.getResourceAsStream("sample-v1.json")) {
            return in.readAllBytes();
        }
    }

    private static ObjectNode migrated(String json) throws Exception {
        return Migrator.standard().migrate((ObjectNode) MAPPER.readTree(json));
    }

    @Test
    void sampleFileInVersionOneGivesCurrentSampleData() throws Exception {
        AppData data = new JsonStorage(dir, CLOCK).parse(sampleV1());

        assertEquals(TestData.sampleData(), data);
        assertEquals(AppData.CURRENT_SCHEMA_VERSION, data.schemaVersion());
    }

    @Test
    void existingAreasBecomeActive() throws Exception {
        ObjectNode root = migrated("{\"schemaVersion\": 1, \"areas\": [{\"id\": \"a\", \"name\": \"Sen\"}]}");

        assertEquals(2, root.get("schemaVersion").intValue());
        assertFalse(root.get("areas").get(0).get("archived").booleanValue());
    }

    @Test
    void anAlreadyArchivedAreaStaysArchived() throws Exception {
        ObjectNode root = migrated(
                "{\"schemaVersion\": 1, \"areas\": [{\"id\": \"a\", \"name\": \"Sen\", \"archived\": true}]}");

        assertTrue(root.get("areas").get(0).get("archived").booleanValue());
    }

    @Test
    void fileWithoutAreasIsOnlyMarkedAsCurrent() throws Exception {
        ObjectNode root = migrated("{\"schemaVersion\": 1, \"habits\": []}");

        assertEquals(2, root.get("schemaVersion").intValue());
        assertFalse(root.has("areas"));
    }

    @Test
    void oddShapesAreLeftForModelValidation() throws Exception {
        ObjectNode notAnArray = migrated("{\"schemaVersion\": 1, \"areas\": \"Sen\"}");
        ObjectNode notAnObject = migrated("{\"schemaVersion\": 1, \"areas\": [\"Sen\"]}");

        assertEquals("Sen", notAnArray.get("areas").textValue());
        assertEquals("Sen", notAnObject.get("areas").get(0).textValue());
    }

    @Test
    void oldFileIsLoadedAsIsAndBackedUpBeforeTheFirstSaveInTheNewFormat() throws IOException {
        JsonStorage storage = new JsonStorage(dir, CLOCK);
        Files.write(storage.dataFile(), sampleV1());

        LoadResult.Loaded loaded = assertInstanceOf(LoadResult.Loaded.class, storage.load());
        assertEquals(TestData.sampleData(), loaded.data());
        assertArrayEquals(sampleV1(), Files.readAllBytes(storage.dataFile()));

        storage.save(loaded.data());

        assertArrayEquals(sampleV1(), Files.readAllBytes(dir.resolve("backups").resolve("data-2026-10-24.json")));
        assertTrue(Files.readString(storage.dataFile()).contains("\"schemaVersion\": 2"));
        assertEquals(loaded, storage.load());
    }
}
