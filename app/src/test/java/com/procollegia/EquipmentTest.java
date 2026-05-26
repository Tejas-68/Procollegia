package com.procollegia;

import com.procollegia.adapters.EquipmentAdapter;

import org.junit.Test;

import static org.junit.Assert.*;

public class EquipmentTest {

    @Test
    public void constructor_setsFieldsCorrectly() {
        EquipmentAdapter.Equipment eq = 
            new EquipmentAdapter.Equipment("e1", "Basketball", "Sports", 5, "🏀");

        assertEquals("e1", eq.id);
        assertEquals("Basketball", eq.name);
        assertEquals("Sports", eq.category);
        assertEquals(5, eq.available);
        assertEquals("🏀", eq.emoji);
        assertEquals(1, eq.selectedQty); // Initialized to 1
    }
}
