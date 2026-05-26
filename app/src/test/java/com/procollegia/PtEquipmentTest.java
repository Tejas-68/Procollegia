package com.procollegia;

import com.procollegia.adapters.PtEquipmentAdapter;

import org.junit.Test;

import static org.junit.Assert.*;

public class PtEquipmentTest {

    @Test
    public void constructor_setsFieldsCorrectly() {
        PtEquipmentAdapter.PtEquipment item = 
            new PtEquipmentAdapter.PtEquipment("id1", "Cricket Bat", "Cricket", "Available", 10, 8);

        assertEquals("id1", item.id);
        assertEquals("Cricket Bat", item.name);
        assertEquals("Cricket", item.category);
        assertEquals("Available", item.status);
        assertEquals(10, item.quantity);
        assertEquals(8, item.remaining);
    }
}
