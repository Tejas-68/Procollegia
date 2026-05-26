package com.procollegia;

import com.procollegia.models.TimetableSession;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link TimetableSession}.
 *
 * Covers:
 *  - Constructor correctly storing all fields
 *  - isValid() for complete and incomplete sessions
 *  - getDisplayLabel() formatting
 *  - Null / blank field edge cases
 *
 * Run with:  ./gradlew testDebugUnitTest
 */
public class TimetableSessionTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Constructor & Getters
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void constructor_storesAllFields() {
        TimetableSession session = new TimetableSession("09:00", "Maths", "Lab 3", "Dr. Smith");

        assertEquals("09:00", session.getTime());
        assertEquals("Maths", session.getSubject());
        assertEquals("Lab 3", session.getRoom());
        assertEquals("Dr. Smith", session.getLecturer());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // isValid()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void completeSession_isValid() {
        TimetableSession session = new TimetableSession("09:00", "Maths", "Lab 3", "Dr. Smith");
        assertTrue(session.isValid());
    }

    @Test
    public void sessionWithNullTime_isInvalid() {
        TimetableSession session = new TimetableSession(null, "Maths", "Lab 3", "Dr. Smith");
        assertFalse(session.isValid());
    }

    @Test
    public void sessionWithEmptyTime_isInvalid() {
        TimetableSession session = new TimetableSession("", "Maths", "Lab 3", "Dr. Smith");
        assertFalse(session.isValid());
    }

    @Test
    public void sessionWithNullSubject_isInvalid() {
        TimetableSession session = new TimetableSession("09:00", null, "Lab 3", "Dr. Smith");
        assertFalse(session.isValid());
    }

    @Test
    public void sessionWithBlankRoom_isInvalid() {
        TimetableSession session = new TimetableSession("09:00", "Maths", "   ", "Dr. Smith");
        assertFalse(session.isValid());
    }

    @Test
    public void sessionWithNullLecturer_isInvalid() {
        TimetableSession session = new TimetableSession("09:00", "Maths", "Lab 3", null);
        assertFalse(session.isValid());
    }

    @Test
    public void allBlankFields_areInvalid() {
        TimetableSession session = new TimetableSession("", "", "", "");
        assertFalse(session.isValid());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getDisplayLabel()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void displayLabel_containsTimeAndSubject() {
        TimetableSession session = new TimetableSession("09:00 - 10:00", "Maths", "Lab 3", "Dr. Smith");
        String label = session.getDisplayLabel();

        assertTrue("Label should contain the time", label.contains("09:00 - 10:00"));
        assertTrue("Label should contain the subject", label.contains("Maths"));
    }

    @Test
    public void displayLabel_withNullTime_doesNotCrash() {
        TimetableSession session = new TimetableSession(null, "Physics", "Room 2", "Dr. Rao");
        String label = session.getDisplayLabel();

        assertNotNull(label);
        assertTrue(label.contains("Physics"));
    }

    @Test
    public void displayLabel_withNullSubject_doesNotCrash() {
        TimetableSession session = new TimetableSession("11:00", null, "Room 2", "Dr. Rao");
        String label = session.getDisplayLabel();

        assertNotNull(label);
        assertTrue(label.contains("11:00"));
    }
}
