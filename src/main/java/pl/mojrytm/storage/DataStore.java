package pl.mojrytm.storage;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import pl.mojrytm.model.AppData;

/**
 * Dane trzymane w pamięci jako niezmienna migawka i zapisywane w tle: kilka zmian pod rząd
 * (w odstępach krótszych niż opóźnienie) to jeden zapis. {@link #update} nigdy nie czeka na dysk,
 * więc można go wołać z wątku JavaFX. Zapis wymuszony przy zamykaniu: {@link #close()}.
 *
 * <p>Nieudany zapis w tle jest zgłaszany przez {@code onSaveError} (wywoływany na wątku zapisu,
 * UI musi sobie przekazać wynik przez Platform.runLater). Zmiany zostają w pamięci i zapis
 * jest ponawiany przy następnej zmianie, {@link #flush()} lub zamknięciu.
 */
public final class DataStore implements AutoCloseable {

    /** Dokąd trafia zapis; w aplikacji {@link JsonStorage#save}. */
    @FunctionalInterface
    public interface Writer {
        void write(AppData data) throws IOException;
    }

    public static final Duration DEFAULT_DELAY = Duration.ofMillis(500);

    private final Writer writer;
    private final ScheduledExecutorService scheduler;
    private final Duration delay;
    private final Consumer<IOException> onSaveError;

    /** Chroni stan w pamięci; trzymana tylko chwilowo, nigdy podczas operacji na dysku. */
    private final Object stateLock = new Object();
    /** Szereguje zapisy, żeby starsza migawka nie wyprzedziła nowszej. */
    private final Object ioLock = new Object();

    private AppData current;
    private boolean dirty;
    private boolean closed;
    private ScheduledFuture<?> pending;

    /** Magazyn zapisujący przez {@code storage} z opóźnieniem {@link #DEFAULT_DELAY}. */
    public static DataStore open(AppData initial, JsonStorage storage, Consumer<IOException> onSaveError) {
        return new DataStore(initial, storage::save, newSaveExecutor(), DEFAULT_DELAY, onSaveError);
    }

    /** Magazyn przejmuje {@code scheduler} i zamyka go razem ze sobą. */
    DataStore(AppData initial, Writer writer, ScheduledExecutorService scheduler, Duration delay,
              Consumer<IOException> onSaveError) {
        this.current = Objects.requireNonNull(initial);
        this.writer = Objects.requireNonNull(writer);
        this.scheduler = Objects.requireNonNull(scheduler);
        this.delay = Objects.requireNonNull(delay);
        this.onSaveError = Objects.requireNonNull(onSaveError);
    }

    private static ScheduledExecutorService newSaveExecutor() {
        return Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "moj-rytm-zapis");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Bieżąca migawka danych. */
    public AppData get() {
        synchronized (stateLock) {
            return current;
        }
    }

    /**
     * Zamienia dane na wynik {@code change} i zaplanuje zapis. Operacja ma być szybka i bez efektów
     * ubocznych; jeśli rzuci wyjątek, dane zostają bez zmian.
     */
    public void update(UnaryOperator<AppData> change) {
        Objects.requireNonNull(change);
        synchronized (stateLock) {
            if (closed) {
                throw new IllegalStateException("Magazyn danych jest już zamknięty");
            }
            AppData next = Objects.requireNonNull(change.apply(current), "Zmiana nie może zwrócić null");
            current = next;
            dirty = true;
            if (pending != null) {
                pending.cancel(false);
            }
            pending = scheduler.schedule(this::saveInBackground, delay.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    /** Zapisuje od razu, jeśli są niezapisane zmiany. Blokuje do końca zapisu. */
    public void flush() throws IOException {
        synchronized (stateLock) {
            if (pending != null) {
                pending.cancel(false);
                pending = null;
            }
        }
        writeIfDirty();
    }

    /** Wymusza zapis i zamyka magazyn. Kolejne wywołania nic nie robią. */
    @Override
    public void close() throws IOException {
        synchronized (stateLock) {
            if (closed) {
                return;
            }
            closed = true;
        }
        try {
            flush();
        } finally {
            scheduler.shutdown();
        }
    }

    private void saveInBackground() {
        try {
            writeIfDirty();
        } catch (IOException e) {
            onSaveError.accept(e);
        } catch (RuntimeException e) {
            // wyjątki z zadania w executorze znikałyby po cichu
            onSaveError.accept(new IOException("Nieoczekiwany błąd zapisu danych", e));
        }
    }

    private void writeIfDirty() throws IOException {
        synchronized (ioLock) {
            AppData snapshot;
            synchronized (stateLock) {
                if (!dirty) {
                    return;
                }
                snapshot = current;
                dirty = false;
            }
            try {
                writer.write(snapshot);
            } catch (IOException | RuntimeException e) {
                synchronized (stateLock) {
                    dirty = true;
                }
                throw e;
            }
        }
    }
}
