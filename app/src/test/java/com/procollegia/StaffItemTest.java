package com.procollegia;

import com.procollegia.adapters.StaffAdapter;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link StaffAdapter.StaffItem}.
 *
 * Covers:
 *  - Initial state after construction
 *  - getSubjectCount() with various list states
 *  - hasSubject() — case-insensitive, trimming, null safety
 *
 * Run with:  ./gradlew testDebugUnitTest
 */
public class StaffItemTest {

    private StaffAdapter.StaffItem staffItem;

    @Before
    public void setUp() {
        List<String> subjects = new ArrayList<>(Arrays.asList("Computer Networks", "DBMS", "C Programming"));
        staffItem = new StaffAdapter.StaffItem("STAFF_001", "Dr. Sameer Khan", subjects);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Construction & Initial State
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void constructor_setsIdCorrectly() {
        assertEquals("STAFF_001", staffItem.id);
    }

    @Test
    public void constructor_setsNameCorrectly() {
        assertEquals("Dr. Sameer Khan", staffItem.name);
    }

    @Test
    public void constructor_isNotExpandedByDefault() {
        assertFalse(staffItem.isExpanded);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getSubjectCount()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void getSubjectCount_returnsCorrectCount() {
        assertEquals(3, staffItem.getSubjectCount());
    }

    @Test
    public void getSubjectCount_withEmptyList_returnsZero() {
        StaffAdapter.StaffItem emptyItem = new StaffAdapter.StaffItem("X", "No Subjects", new ArrayList<>());
        assertEquals(0, emptyItem.getSubjectCount());
    }

    @Test
    public void getSubjectCount_withNullList_returnsZero() {
        StaffAdapter.StaffItem nullItem = new StaffAdapter.StaffItem("X", "Null List", null);
        assertEquals(0, nullItem.getSubjectCount());
    }

    @Test
    public void getSubjectCount_updatesAfterAddingSubject() {
        staffItem.subjects.add("Data Structures");
        assertEquals(4, staffItem.getSubjectCount());
    }

    @Test
    public void getSubjectCount_updatesAfterRemovingSubject() {
        staffItem.subjects.remove("DBMS");
        assertEquals(2, staffItem.getSubjectCount());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // hasSubject()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void hasSubject_returnsTrue_forExistingSubject() {
        assertTrue(staffItem.hasSubject("Computer Networks"));
    }

    @Test
    public void hasSubject_returnsFalse_forNonExistingSubject() {
        assertFalse(staffItem.hasSubject("Machine Learning"));
    }

    @Test
    public void hasSubject_isCaseInsensitive() {
        // "computer networks" should still match "Computer Networks"
        assertTrue(staffItem.hasSubject("computer networks"));
        assertTrue(staffItem.hasSubject("DBMS"));
        assertTrue(staffItem.hasSubject("dbms"));
    }

    @Test
    public void hasSubject_trimsWhitespace() {
        assertTrue(staffItem.hasSubject("  DBMS  "));
    }

    @Test
    public void hasSubject_returnsFalse_forNullInput() {
        assertFalse(staffItem.hasSubject(null));
    }

    @Test
    public void hasSubject_returnsFalse_forEmptyString() {
        assertFalse(staffItem.hasSubject(""));
    }

    @Test
    public void hasSubject_returnsFalse_whenSubjectListIsNull() {
        StaffAdapter.StaffItem nullItem = new StaffAdapter.StaffItem("X", "Test", null);
        assertFalse(nullItem.hasSubject("DBMS"));
    }

    @Test
    public void hasSubject_returnsFalse_afterSubjectRemoved() {
        staffItem.subjects.remove("DBMS");
        assertFalse(staffItem.hasSubject("DBMS"));
    }

    @Test
    public void hasSubject_returnsTrue_afterSubjectAdded() {
        staffItem.subjects.add("Operating Systems");
        assertTrue(staffItem.hasSubject("Operating Systems"));
    }
}
