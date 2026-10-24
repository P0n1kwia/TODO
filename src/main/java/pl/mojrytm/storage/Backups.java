package pl.mojrytm.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Dzienne kopie zapasowe w folderze backups/ (data-RRRR-MM-DD.json), ostatnie 14. */
final class Backups {

    static final int KEEP = 14;

    private static final Pattern NAME = Pattern.compile("data-(\\d{4}-\\d{2}-\\d{2})\\.json");

    private final Path dir;
    private final Clock clock;

    Backups(Path dir, Clock clock) {
        this.dir = dir;
        this.clock = clock;
    }

    boolean existsForToday() {
        return Files.exists(fileFor(LocalDate.now(clock)));
    }

    /** Zapisuje kopię z dzisiejszą datą (atomowo, przez plik tymczasowy). */
    void createForToday(byte[] content) throws IOException {
        Files.createDirectories(dir);
        Path target = fileFor(LocalDate.now(clock));
        Path tmp = dir.resolve(target.getFileName() + ".tmp");
        Files.write(tmp, content);
        Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
    }

    /** Najnowsza kopia (po dacie w nazwie), jeśli jakakolwiek istnieje. */
    Optional<Path> latest() throws IOException {
        List<Dated> all = list();
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0).file());
    }

    /** Usuwa kopie ponad limit, od najstarszych. Nie rusza plików o obcych nazwach. */
    void prune() throws IOException {
        List<Dated> all = list();
        for (Dated old : all.subList(Math.min(KEEP, all.size()), all.size())) {
            Files.deleteIfExists(old.file());
        }
    }

    private Path fileFor(LocalDate date) {
        return dir.resolve("data-" + date + ".json");
    }

    /** Kopie od najnowszej do najstarszej. */
    private List<Dated> list() throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<Dated> result = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                Matcher matcher = NAME.matcher(file.getFileName().toString());
                if (matcher.matches() && Files.isRegularFile(file)) {
                    try {
                        result.add(new Dated(LocalDate.parse(matcher.group(1)), file));
                    } catch (DateTimeParseException ignored) {
                        // nazwa podobna do kopii, ale z nieistniejącą datą: ignorujemy
                    }
                }
            }
        }
        result.sort(Comparator.comparing(Dated::date).reversed());
        return result;
    }

    private record Dated(LocalDate date, Path file) {
    }
}
