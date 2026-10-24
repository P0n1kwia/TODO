package pl.mojrytm.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import pl.mojrytm.model.AppData;

/**
 * Odczyt i zapis pliku danych w jednym folderze. Zapis jest atomowy: najpierw plik tymczasowy,
 * potem przeniesienie na miejsce, nigdy zapis wprost do data.json. Klasa nie jest wątkowo
 * bezpieczna; zapisy w tle porządkuje {@link DataStore}.
 */
public final class JsonStorage {

    static final String DATA_FILE = "data.json";
    static final String TMP_FILE = "data.json.tmp";

    private static final DateTimeFormatter CORRUPT_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path dir;
    private final Clock clock;
    private final Migrator migrator;
    private final ObjectMapper mapper = Json.newMapper();

    public JsonStorage(Path dir, Clock clock) {
        this(dir, clock, Migrator.standard());
    }

    JsonStorage(Path dir, Clock clock, Migrator migrator) {
        this.dir = dir;
        this.clock = clock;
        this.migrator = migrator;
    }

    public Path dataFile() {
        return dir.resolve(DATA_FILE);
    }

    /**
     * Otwiera plik danych. Brak pliku to puste dane. Uszkodzony plik jest przenoszony na bok,
     * plik z nowszej wersji zostaje nietknięty. Błąd samego odczytu (np. brak uprawnień)
     * jest zgłaszany jako {@link IOException}, bo nie świadczy o uszkodzeniu danych.
     */
    public LoadResult load() throws IOException {
        Path file = dataFile();
        if (!Files.exists(file)) {
            return new LoadResult.Loaded(AppData.empty());
        }
        byte[] bytes = Files.readAllBytes(file);
        try {
            return new LoadResult.Loaded(parse(bytes));
        } catch (NewerSchemaVersionException e) {
            return new LoadResult.NewerVersion(e.fileVersion(), e.appVersion());
        } catch (DataFormatException e) {
            return new LoadResult.Corrupt(moveAsideAsCorrupt(file), e.getMessage());
        }
    }

    /** Zapisuje dane atomowo. */
    public void save(AppData data) throws IOException {
        Files.createDirectories(dir);
        byte[] bytes = (mapper.writeValueAsString(data) + "\n").getBytes(StandardCharsets.UTF_8);
        Path tmp = dir.resolve(TMP_FILE);
        try (FileChannel channel = FileChannel.open(tmp,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
        Files.move(tmp, dataFile(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Zamienia bajty pliku na dane: parsowanie, migracje, walidacja modelu. */
    AppData parse(byte[] bytes) throws DataFormatException {
        JsonNode root;
        try {
            root = mapper.readTree(bytes);
        } catch (IOException e) {
            throw new DataFormatException("Plik nie jest poprawnym JSON-em: " + messageOf(e), e);
        }
        if (root == null || !root.isObject()) {
            throw new DataFormatException("Plik nie zawiera obiektu JSON");
        }
        ObjectNode migrated = migrator.migrate((ObjectNode) root);
        try {
            return mapper.treeToValue(migrated, AppData.class);
        } catch (JsonProcessingException e) {
            throw new DataFormatException("Dane są nieprawidłowe: " + messageOf(e), e);
        }
    }

    private static String messageOf(IOException e) {
        return e instanceof JsonProcessingException json ? json.getOriginalMessage() : e.getMessage();
    }

    private Path moveAsideAsCorrupt(Path file) throws IOException {
        String stamp = LocalDateTime.now(clock).format(CORRUPT_STAMP);
        Path target = dir.resolve("data.corrupt-" + stamp + ".json");
        for (int i = 1; Files.exists(target); i++) {
            target = dir.resolve("data.corrupt-" + stamp + "-" + i + ".json");
        }
        // bez REPLACE_EXISTING: uszkodzonego pliku nie wolno stracić
        return Files.move(file, target);
    }
}
