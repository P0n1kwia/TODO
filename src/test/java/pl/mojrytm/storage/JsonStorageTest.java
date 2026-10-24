package pl.mojrytm.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.mojrytm.TestData;
import pl.mojrytm.model.AppData;

class JsonStorageTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-24T10:30:00Z"), ZoneOffset.UTC);

    @TempDir
    Path dir;

    private JsonStorage storage;

    @BeforeEach
    void setUp() {
        storage = new JsonStorage(dir, CLOCK);
    }

    private List<String> fileNames() throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    private Path writeRaw(String content) throws IOException {
        Path file = storage.dataFile();
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void missingFileGivesEmptyDataAndCreatesNothing() throws IOException {
        LoadResult result = storage.load();

        assertEquals(new LoadResult.Loaded(AppData.empty()), result);
        assertEquals(List.of(), fileNames());
    }

    @Test
    void savedDataIsReadBackUnchanged() throws IOException {
        storage.save(TestData.sampleData());

        assertEquals(new LoadResult.Loaded(TestData.sampleData()), storage.load());
    }

    @Test
    void saveEndsWithNewlineAndLeavesNoTemporaryFile() throws IOException {
        storage.save(TestData.sampleData());

        assertTrue(Files.readString(storage.dataFile()).endsWith("}\n"));
        assertEquals(List.of("data.json"), fileNames());
    }

    @Test
    void saveCreatesMissingFolders() throws IOException {
        Path nested = dir.resolve("a/b/c");

        new JsonStorage(nested, CLOCK).save(AppData.empty());

        assertTrue(Files.exists(nested.resolve("data.json")));
    }

    @Test
    void saveReplacesPreviousContent() throws IOException {
        storage.save(TestData.sampleData());
        storage.save(AppData.empty());

        assertEquals(new LoadResult.Loaded(AppData.empty()), storage.load());
    }

    @Test
    void failedWriteLeavesPreviousFileIntact() throws IOException {
        storage.save(TestData.sampleData());
        byte[] before = Files.readAllBytes(storage.dataFile());
        // katalog w miejscu pliku tymczasowego uniemożliwia zapis
        Files.createDirectory(dir.resolve("data.json.tmp"));

        assertThrows(IOException.class, () -> storage.save(AppData.empty()));

        assertArrayEquals(before, Files.readAllBytes(storage.dataFile()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "to nie jest json",
            "{\"schemaVersion\": 1, \"habits\": [",
            "[1, 2, 3]",
            "{\"schemaVersion\": 1} śmieci po końcu",
            "{\"areas\": []}",
            "{\"schemaVersion\": \"1\"}",
            "{\"schemaVersion\": 1, \"checkIns\": [{\"date\": \"2026-10-24\", \"mood\": 9, \"energy\": 3}]}",
            "{\"schemaVersion\": 1, \"habits\": [{\"id\": \"h\", \"name\": \"X\", \"kind\": \"INNY\","
                    + " \"schedule\": \"DAILY\", \"created\": \"2026-10-01\"}]}",
            "{\"schemaVersion\": 1, \"entries\": [{\"id\": \"e\", \"habitId\": \"h\", \"date\": \"wczoraj\"}]}"
    })
    void corruptFileIsMovedAsideNotOverwritten(String content) throws IOException {
        writeRaw(content);

        LoadResult result = storage.load();

        LoadResult.Corrupt corrupt = assertInstanceOf(LoadResult.Corrupt.class, result);
        assertEquals(dir.resolve("data.corrupt-20261024-103000.json"), corrupt.corruptFile());
        assertEquals(content, Files.readString(corrupt.corruptFile()));
        assertFalse(Files.exists(storage.dataFile()));
        assertFalse(corrupt.reason().isBlank());
    }

    @Test
    void secondCorruptFileInTheSameSecondGetsDifferentName() throws IOException {
        writeRaw("pierwszy");
        LoadResult.Corrupt first = assertInstanceOf(LoadResult.Corrupt.class, storage.load());
        writeRaw("drugi");
        LoadResult.Corrupt second = assertInstanceOf(LoadResult.Corrupt.class, storage.load());

        assertEquals("pierwszy", Files.readString(first.corruptFile()));
        assertEquals("drugi", Files.readString(second.corruptFile()));
    }

    @Test
    void newerSchemaIsRefusedAndNothingChanges() throws IOException {
        String content = "{\"schemaVersion\": 99, \"nowePole\": true}\n";
        writeRaw(content);

        LoadResult result = storage.load();

        assertEquals(new LoadResult.NewerVersion(99, AppData.CURRENT_SCHEMA_VERSION), result);
        assertEquals(content, Files.readString(storage.dataFile()));
        assertEquals(List.of("data.json"), fileNames());
    }

    @Test
    void olderFileIsMigratedOnLoad() throws IOException {
        Migration renameHabitList = new Migration() {
            @Override
            public int fromVersion() {
                return 0;
            }

            @Override
            public void migrate(ObjectNode root) {
                root.set("areas", root.remove("obszary"));
            }
        };
        JsonStorage migrating = new JsonStorage(dir, CLOCK, new Migrator(1, List.of(renameHabitList)));
        writeRaw("{\"schemaVersion\": 0, \"obszary\": [{\"id\": \"a\", \"name\": \"Sen\"}]}");

        LoadResult.Loaded loaded = assertInstanceOf(LoadResult.Loaded.class, migrating.load());

        assertEquals(1, loaded.data().schemaVersion());
        assertEquals("Sen", loaded.data().areas().get(0).name());
    }

    @Test
    void ioErrorWhileReadingIsNotTreatedAsCorruption() throws IOException {
        // katalog w miejscu pliku: istnieje, ale nie da się go przeczytać jak plik
        Files.createDirectory(storage.dataFile());

        assertThrows(IOException.class, () -> storage.load());

        assertTrue(Files.isDirectory(storage.dataFile()));
        assertEquals(List.of("data.json"), fileNames());
    }
}
