package pl.mojrytm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class TagsTest {

    @Test
    void parseSplitsOnCommasTrimsAndLowercases() {
        assertEquals(List.of("sen", "energia"), Tags.parse("  Sen ,ENERGIA "));
    }

    @Test
    void parseDropsHashesBlanksAndDuplicatesKeepingTheFirstOccurrence() {
        assertEquals(List.of("sen", "praca"), Tags.parse("#sen, ,##Praca, SEN, #"));
    }

    @Test
    void polishLettersAreLowercasedToo() {
        assertEquals(List.of("łóżko", "żółć"), Tags.parse("ŁÓŻKO, Żółć"));
    }

    @Test
    void spacesInsideATagStay() {
        assertEquals(List.of("zdrowie psychiczne"), Tags.parse("Zdrowie psychiczne"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", ",", " , , "})
    void nothingUsefulGivesNoTags(String text) {
        assertEquals(List.of(), Tags.parse(text));
    }

    @Test
    void normalizeIgnoresNullElements() {
        assertEquals(List.of("a", "b"), Tags.normalize(Arrays.asList("A", null, " b ", "a")));
    }
}
