package dev.maire.nourished.api.impl;

import dev.maire.nourished.api.impl.RegistrationBatch.Kind;
import dev.marie.framework.api.value.ValueDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationBatchTest {

    private static ValueDefinition def(String id) {
        return ValueDefinition.builder(id).displayName(id).build();
    }

    @Test
    void stagesOnlyWhileOpenAndTracksOwner() {
        assertFalse(RegistrationBatch.stage(Kind.NUTRIENT, def("before")));

        RegistrationBatch batch = RegistrationBatch.open();
        try {
            batch.setOwner("addon_a");
            assertTrue(RegistrationBatch.stage(Kind.NUTRIENT, def("during")));
        } finally {
            batch.close();
        }

        assertFalse(RegistrationBatch.stage(Kind.NUTRIENT, def("after")));
        assertEquals(1, batch.entries().size());
        assertEquals("addon_a", batch.entries().get(0).owner());
    }

    @Test
    void lateSubmitOnHeldBatchThrows() {
        RegistrationBatch batch = RegistrationBatch.open();
        batch.close();

        assertThrows(IllegalStateException.class, () -> batch.submit(Kind.NUTRIENT, def("late")));
    }
}
