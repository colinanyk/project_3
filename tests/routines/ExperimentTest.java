package routines;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExperimentTest {
    @Test
    void constructorStoresAllValues() {
        Experiment experiment = new Experiment("Soil analysis", 120, 9);

        assertAll(
                () -> assertEquals("Soil analysis", experiment.getName()),
                () -> assertEquals(120, experiment.getWeight()),
                () -> assertEquals(9, experiment.getRating())
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "Experiment with spaces", "Temperature Δ"})
    void nameIsReturnedUnchanged(String name) {
        Experiment experiment = new Experiment(name, 100, 5);

        assertEquals(name, experiment.getName());
    }

    // The current constructor stores values without imposing validation rules.
    @ParameterizedTest
    @CsvSource({"0, 0", "700, 10", "701, 11", "-1, -1",
            "2147483647, 2147483647", "-2147483648, -2147483648"})
    void numericValuesAreReturnedUnchanged(int weight, int rating) {
        Experiment experiment = new Experiment("Boundary test", weight, rating);

        assertAll(
                () -> assertEquals(weight, experiment.getWeight()),
                () -> assertEquals(rating, experiment.getRating())
        );
    }

    @Test
    void instancesKeepIndependentValues() {
        Experiment first = new Experiment("First", 100, 3);
        Experiment second = new Experiment("Second", 200, 8);

        assertAll(
                () -> assertEquals("First", first.getName()),
                () -> assertEquals(100, first.getWeight()),
                () -> assertEquals(3, first.getRating()),
                () -> assertEquals("Second", second.getName()),
                () -> assertEquals(200, second.getWeight()),
                () -> assertEquals(8, second.getRating())
        );
    }
}
