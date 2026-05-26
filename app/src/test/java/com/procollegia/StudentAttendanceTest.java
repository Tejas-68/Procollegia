package com.procollegia;

import com.procollegia.adapters.StudentAttendanceAdapter;

import org.junit.Test;

import static org.junit.Assert.*;

public class StudentAttendanceTest {

    @Test
    public void constructor_setsFieldsCorrectly() {
        StudentAttendanceAdapter.StudentAttendance student = 
            new StudentAttendanceAdapter.StudentAttendance("uid1", "Alice", "UUCMS123", "1st Year", "Sec A");

        assertEquals("uid1", student.id);
        assertEquals("Alice", student.name);
        assertEquals("UUCMS123", student.uucmsId);
        assertEquals("1st Year", student.year);
        assertEquals("Sec A", student.section);
        assertEquals("P", student.status); // Default should be Present
        assertFalse(student.isOnLeave); // Default should be false
    }

    @Test
    public void constructor_handlesNullName() {
        StudentAttendanceAdapter.StudentAttendance student = 
            new StudentAttendanceAdapter.StudentAttendance("uid1", null, "UUCMS123", "1st Year", "Sec A");

        assertEquals("Unknown", student.name);
    }

    @Test
    public void constructor_handlesNullUucmsId() {
        StudentAttendanceAdapter.StudentAttendance student = 
            new StudentAttendanceAdapter.StudentAttendance("uid1", "Alice", null, "1st Year", "Sec A");

        assertEquals("N/A", student.uucmsId);
    }

    @Test
    public void constructor_handlesNullYear() {
        StudentAttendanceAdapter.StudentAttendance student = 
            new StudentAttendanceAdapter.StudentAttendance("uid1", "Alice", "UUCMS123", null, "Sec A");

        assertEquals("", student.year);
    }

    @Test
    public void constructor_handlesNullSection() {
        StudentAttendanceAdapter.StudentAttendance student = 
            new StudentAttendanceAdapter.StudentAttendance("uid1", "Alice", "UUCMS123", "1st Year", null);

        assertEquals("", student.section);
    }
}
