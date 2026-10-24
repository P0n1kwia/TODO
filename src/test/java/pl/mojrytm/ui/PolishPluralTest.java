package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PolishPluralTest {

    @ParameterizedTest
    @CsvSource({
            "0, many", "1, one", "2, few", "3, few", "4, few", "5, many", "9, many", "10, many",
            "11, many", "12, many", "13, many", "14, many", "15, many", "19, many", "20, many",
            "21, many", "22, few", "23, few", "24, few", "25, many", "100, many", "101, many",
            "102, few", "111, many", "112, many", "114, many", "122, few", "1000, many"
    })
    void picksTheRightForm(int count, String expected) {
        assertEquals(expected, PolishPlural.pick(count, "one", "few", "many"));
    }
}
