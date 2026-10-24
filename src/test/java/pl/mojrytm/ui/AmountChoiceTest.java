package pl.mojrytm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AmountChoiceTest {

    @Test
    void parsesAnAmountWithACommaAndKeepsTheMinimalFlag() {
        assertEquals(Optional.of(new AmountChoice(2.5, true)), AmountChoice.parse("2,5", true));
    }

    @Test
    void zeroIsAllowedBecauseItClearsTheDay() {
        assertEquals(Optional.of(new AmountChoice(0, false)), AmountChoice.parse("0", false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "-1", "NaN", "Infinity"})
    void rejectsTextThatIsNotANonNegativeNumber(String text) {
        assertEquals(Optional.empty(), AmountChoice.parse(text, false));
    }
}
