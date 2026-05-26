package com.procollegia;

import com.procollegia.adapters.HonorEventAdapter;

import org.junit.Test;

import static org.junit.Assert.*;

public class HonorEventTest {

    @Test
    public void constructor_setsFieldsCorrectly() {
        HonorEventAdapter.HonorEvent event = 
            new HonorEventAdapter.HonorEvent(50, "Won hackathon", "2026-04-03");

        assertEquals(50, event.points);
        assertEquals("Won hackathon", event.description);
        assertEquals("2026-04-03", event.date);
    }

    @Test
    public void constructor_handlesNegativePoints() {
        HonorEventAdapter.HonorEvent event = 
            new HonorEventAdapter.HonorEvent(-20, "Late submission", "2026-04-04");

        assertEquals(-20, event.points);
        assertEquals("Late submission", event.description);
        assertEquals("2026-04-04", event.date);
    }
}
