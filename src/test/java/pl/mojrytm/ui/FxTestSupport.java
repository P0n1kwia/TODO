package pl.mojrytm.ui;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javafx.application.Platform;
import org.junit.jupiter.api.Assumptions;

/**
 * Pomoc dla testów widoków. Potrzebny jest prawdziwy toolkit JavaFX (ekran); gdy go nie ma
 * (np. serwer bez wyświetlacza), testy widoków są pomijane, a nie oblewane.
 */
final class FxTestSupport {

    private static Boolean available;

    private FxTestSupport() {
    }

    /** Wołać w @BeforeAll testów widoków. */
    static void assumeToolkit() {
        Assumptions.assumeTrue(start(), "Brak ekranu dla JavaFX: pomijam test widoków");
    }

    private static synchronized boolean start() {
        if (available == null) {
            available = tryStart();
        }
        return available;
    }

    private static boolean tryStart() {
        boolean linux = System.getProperty("os.name", "").toLowerCase().contains("linux");
        if (linux && System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null) {
            return false;
        }
        try {
            CountDownLatch started = new CountDownLatch(1);
            Platform.startup(started::countDown);
            return started.await(10, TimeUnit.SECONDS);
        } catch (Throwable e) {
            return false;
        }
    }

    /** Wykonuje akcję na wątku JavaFX i zwraca jej wynik (wyjątki są rzucane dalej). */
    static <T> T onFx(Supplier<T> action) {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(action.get());
            } catch (Throwable e) {
                failure.set(e);
            } finally {
                done.countDown();
            }
        });
        try {
            if (!done.await(20, TimeUnit.SECONDS)) {
                throw new AssertionError("Wątek JavaFX nie odpowiedział w 20 s");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
        if (failure.get() != null) {
            throw new AssertionError("Błąd na wątku JavaFX", failure.get());
        }
        return result.get();
    }

    static void onFx(Runnable action) {
        onFx(() -> {
            action.run();
            return null;
        });
    }
}
