package com.procollegia;

import com.procollegia.adapters.TournamentAdapter;

import org.junit.Test;

import static org.junit.Assert.*;

public class TournamentItemTest {

    @Test
    public void standardConstructor_setsFieldsCorrectly() {
        TournamentAdapter.TournamentItem item = 
            new TournamentAdapter.TournamentItem("t1", "Spring Cup", "Basketball", "Main Court", "2026-04-10", "8", "$500", true, "🏀");

        assertEquals("t1", item.id);
        assertEquals("Spring Cup", item.name);
        assertEquals("Basketball", item.type);
        assertEquals("Main Court", item.venue);
        assertEquals("2026-04-10", item.date);
        assertEquals("8", item.teams);
        assertEquals("$500", item.prize);
        assertTrue(item.isLive);
        assertEquals("🏀", item.emoji);
        assertEquals("ongoing", item.status);
        assertEquals("Solo", item.gameType);
        assertEquals(1, item.maxPlayers);
    }

    @Test
    public void standardConstructor_setsStatusUpcoming_whenNotLive() {
        TournamentAdapter.TournamentItem item = 
            new TournamentAdapter.TournamentItem("t1", "Spring Cup", "Basketball", "Main Court", "2026-04-10", "8", "$500", false, "🏀");

        assertFalse(item.isLive);
        assertEquals("upcoming", item.status);
    }

    @Test
    public void ptConstructor_setsFieldsCorrectly() {
        TournamentAdapter.TournamentItem item = 
            new TournamentAdapter.TournamentItem("t2", "Winter Games", "Football", "2026-12-01", "Stadium", "ongoing");

        assertEquals("t2", item.id);
        assertEquals("Winter Games", item.name);
        assertEquals("Football", item.type);
        assertEquals("2026-12-01", item.date);
        assertEquals("Stadium", item.venue);
        assertEquals("ongoing", item.status);
        assertTrue(item.isLive);
        assertEquals("🏆", item.emoji); // Default emoji
        assertEquals("—", item.teams); // Default teams
        assertEquals("Trophy", item.prize); // Default prize
        assertEquals("Solo", item.gameType);
        assertEquals(1, item.maxPlayers);
    }

    @Test
    public void ptConstructor_isLiveFalse_whenStatusUpcoming() {
        TournamentAdapter.TournamentItem item = 
            new TournamentAdapter.TournamentItem("t2", "Winter Games", "Football", "2026-12-01", "Stadium", "Upcoming");

        assertEquals("Upcoming", item.status);
        assertFalse(item.isLive); // Case insensitive match for "ongoing" should fail
    }

    @Test
    public void ptConstructor_isLiveTrue_whenStatusOngoingCaseInsensitive() {
        TournamentAdapter.TournamentItem item = 
            new TournamentAdapter.TournamentItem("t2", "Winter Games", "Football", "2026-12-01", "Stadium", "ONGOING");

        assertEquals("ONGOING", item.status);
        assertTrue(item.isLive); // Case insensitive match for "ongoing" should pass
    }
}
