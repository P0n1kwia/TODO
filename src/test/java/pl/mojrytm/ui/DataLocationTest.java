package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DataLocationTest {

    private final String original = System.getProperty(DataLocation.OVERRIDE_PROPERTY);

    @AfterEach
    void restoreProperty() {
        if (original == null) {
            System.clearProperty(DataLocation.OVERRIDE_PROPERTY);
        } else {
            System.setProperty(DataLocation.OVERRIDE_PROPERTY, original);
        }
    }

    @Test
    void defaultIsDotMojRytmInTheHomeFolder() {
        System.clearProperty(DataLocation.OVERRIDE_PROPERTY);

        assertEquals(Path.of(System.getProperty("user.home"), ".moj-rytm"), DataLocation.resolve());
    }

    @Test
    void systemPropertyOverridesTheDefault() {
        System.setProperty(DataLocation.OVERRIDE_PROPERTY, "/tmp/inne-dane");

        assertEquals(Path.of("/tmp/inne-dane"), DataLocation.resolve());
    }

    @Test
    void blankOverrideIsIgnored() {
        System.setProperty(DataLocation.OVERRIDE_PROPERTY, "  ");

        assertEquals(Path.of(System.getProperty("user.home"), ".moj-rytm"), DataLocation.resolve());
    }
}
