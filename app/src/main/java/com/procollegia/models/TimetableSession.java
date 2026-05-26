package com.procollegia.models;

public class TimetableSession {
    private String time;
    private String subject;
    private String room;
    private String lecturer;

    public TimetableSession(String time, String subject, String room, String lecturer) {
        this.time = time;
        this.subject = subject;
        this.room = room;
        this.lecturer = lecturer;
    }

    public String getTime() { return time; }
    public String getSubject() { return subject; }
    public String getRoom() { return room; }
    public String getLecturer() { return lecturer; }

    /**
     * Returns true when none of the four fields are null or blank.
     * Use this to guard against incomplete session data before display.
     */
    public boolean isValid() {
        return isNonEmpty(time) && isNonEmpty(subject)
                && isNonEmpty(room) && isNonEmpty(lecturer);
    }

    /**
     * Returns a short human-readable label, e.g. "09:00 - 10:00 — Maths".
     * Useful for accessibility descriptions and notification text.
     */
    public String getDisplayLabel() {
        return (time != null ? time : "") + " — " + (subject != null ? subject : "");
    }

    private static boolean isNonEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
