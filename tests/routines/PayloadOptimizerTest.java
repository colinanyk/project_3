package routines;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PayloadOptimizerTest {
    @Test
    void constructorCreatesAnOptimizer() {
        assertDoesNotThrow(() -> new PayloadOptimizer());
    }

    @Test
    void maxWeightIs700() {
        assertEquals(700, PayloadOptimizer.getMaxWeight());
    }

    @Test
    void creatingInstancesDoesNotChangeMaxWeight() {
        new PayloadOptimizer();
        new PayloadOptimizer();

        assertEquals(700, PayloadOptimizer.getMaxWeight());
    }
}
