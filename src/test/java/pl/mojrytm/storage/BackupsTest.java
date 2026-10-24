package pl.mojrytm.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.mojrytm.TestData;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Settings;

class BackupsTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 24);

    @TempDir
    Path dir;

    private static Clock clockOn(LocalDate day) {
        return Clock.fixed(day.atTime(10, 30).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    private JsonStorage storageOn(LocalDate day) {
        return new JsonStorage(dir, clockOn(day));
    }

    private Path backup(LocalDate day) {
        return dir.resolve("backups").resolve("data-" + day + ".json");
    }

    private List<String> backupNames() throws IOException {
        Path backups = dir.resolve("backups");
        if (!Files.isDirectory(backups)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(backups)) {
            return files.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    /** Dane różniące się między dniami, żeby kopie dało się odróżnić. */
    private static AppData dataVariant(int variant) {
        return new AppData(1, new Settings(variant % 24, null),
                null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void firstEverSaveMakesNoBackup() throws IOException {
        storageOn(DAY).save(TestData.sampleData());

        assertEquals(List.of(), backupNames());
    }

    @Test
    void firstSaveOfTheDayBacksUpThePreviousFileAndLaterOnesDoNot() throws IOException {
        JsonStorage storage = storageOn(DAY);
        storage.save(TestData.sampleData());
        byte[] stateBeforeSecondSave = Files.readAllBytes(storage.dataFile());

        storage.save(AppData.empty());
        storage.save(dataVariant(7));

        assertEquals(List.of("data-2026-10-24.json"), backupNames());
        assertArrayEquals(stateBeforeSecondSave, Files.readAllBytes(backup(DAY)));
    }

    @Test
    void nextDayGetsItsOwnBackup() throws IOException {
        storageOn(DAY).save(dataVariant(1));
        storageOn(DAY).save(dataVariant(2));
        storageOn(DAY.plusDays(1)).save(dataVariant(3));

        assertEquals(List.of("data-2026-10-24.json", "data-2026-10-25.json"), backupNames());
        assertEquals(dataVariant(1), storageOn(DAY).parse(Files.readAllBytes(backup(DAY))));
        assertEquals(dataVariant(2), storageOn(DAY).parse(Files.readAllBytes(backup(DAY.plusDays(1)))));
    }

    @Test
    void keepsOnlyTheNewestFourteenBackups() throws IOException {
        for (int i = 0; i < 20; i++) {
            storageOn(DAY.plusDays(i)).save(dataVariant(i));
        }

        List<String> names = backupNames();

        // dzień 0 nie ma kopii (nie było poprzedniego pliku), kopie z dni 1–19 przycięte do 14 najnowszych
        assertEquals(14, names.size());
        assertEquals("data-" + DAY.plusDays(6) + ".json", names.get(0));
        assertEquals("data-" + DAY.plusDays(19) + ".json", names.get(13));
    }

    @Test
    void pruningDoesNotTouchForeignFiles() throws IOException {
        Path backups = Files.createDirectories(dir.resolve("backups"));
        List<String> foreign = List.of("notatki.txt", "data-abc.json", "data-2026-10-01.json.tmp",
                "data-2026-13-45.json", "data-2026-10-01-stara.json");
        for (String name : foreign) {
            Files.writeString(backups.resolve(name), "x");
        }

        for (int i = 0; i < 17; i++) {
            storageOn(DAY.plusDays(i)).save(dataVariant(i));
        }

        for (String name : foreign) {
            assertTrue(Files.exists(backups.resolve(name)), name);
        }
        assertEquals(foreign.size() + 14, backupNames().size());
    }

    @Test
    void unreadableCurrentFileIsNotCopiedToBackups() throws IOException {
        JsonStorage storage = storageOn(DAY);
        storage.save(dataVariant(1));
        Files.writeString(storage.dataFile(), "uszkodzone w trakcie sesji");

        storage.save(dataVariant(2));

        assertEquals(List.of(), backupNames());
        assertEquals(new LoadResult.Loaded(dataVariant(2)), storage.load());
    }

    @Test
    void failingBackupDoesNotBlockSaving() throws IOException {
        // plik w miejscu folderu kopii uniemożliwia ich tworzenie
        Files.writeString(dir.resolve("backups"), "to nie jest folder");
        JsonStorage storage = storageOn(DAY);
        storage.save(dataVariant(1));

        storage.save(dataVariant(2));

        assertEquals(new LoadResult.Loaded(dataVariant(2)), storage.load());
    }

    @Test
    void latestBackupIsTheNewestByDateInName() throws IOException {
        JsonStorage storage = storageOn(DAY.plusDays(2));
        assertEquals(Optional.empty(), storage.latestBackup());
        storageOn(DAY).save(dataVariant(1));
        storageOn(DAY).save(dataVariant(2));
        storageOn(DAY.plusDays(2)).save(dataVariant(3));
        storageOn(DAY.plusDays(1)).save(dataVariant(4));

        assertEquals(Optional.of(backup(DAY.plusDays(2))), storage.latestBackup());
    }

    @Test
    void corruptFileResultOffersTheLatestBackup() throws IOException {
        JsonStorage storage = storageOn(DAY);
        storage.save(dataVariant(1));
        storage.save(dataVariant(2));
        Files.writeString(storage.dataFile(), "{ uszkodzony");

        LoadResult.Corrupt corrupt = assertInstanceOf(LoadResult.Corrupt.class, storage.load());

        assertEquals(Optional.of(backup(DAY)), corrupt.latestBackup());
    }

    @Test
    void corruptFileWithoutAnyBackupOffersNothing() throws IOException {
        JsonStorage storage = storageOn(DAY);
        Files.writeString(storage.dataFile(), "{ uszkodzony");

        LoadResult.Corrupt corrupt = assertInstanceOf(LoadResult.Corrupt.class, storage.load());

        assertEquals(Optional.empty(), corrupt.latestBackup());
    }

    @Test
    void restoringFromBackupBringsDataBack() throws IOException {
        JsonStorage storage = storageOn(DAY);
        storage.save(TestData.sampleData());
        storage.save(AppData.empty());
        Files.writeString(storage.dataFile(), "{ uszkodzony");
        LoadResult.Corrupt corrupt = assertInstanceOf(LoadResult.Corrupt.class, storage.load());

        AppData restored = storage.restoreFromBackup(corrupt.latestBackup().orElseThrow());

        assertEquals(TestData.sampleData(), restored);
        assertEquals(new LoadResult.Loaded(TestData.sampleData()), storage.load());
        assertTrue(Files.exists(corrupt.corruptFile()), "uszkodzony plik zostaje do wglądu");
    }

    @Test
    void restoringFromInvalidBackupChangesNothing() throws IOException {
        JsonStorage storage = storageOn(DAY);
        Path bad = Files.writeString(dir.resolve("zla-kopia.json"), "nie json");

        assertThrows(DataFormatException.class, () -> storage.restoreFromBackup(bad));

        assertFalse(Files.exists(storage.dataFile()));
    }
}
