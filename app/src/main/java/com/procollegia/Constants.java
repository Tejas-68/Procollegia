package com.procollegia;

/**
 * Central repository of Firestore collection name constants.
 * Use these everywhere instead of bare string literals to prevent typo-bugs.
 *
 * Root cause of several P1 bugs was inconsistent collection names across files.
 */
public final class Constants {

    private Constants() {} // prevent instantiation

    // ── Firestore collections ─────────────────────────────────────────────────

    /** All user profiles (Student, Teacher, HOD, PT Admin, Principal) */
    public static final String COL_USERS = "users";

    /** Per-student attendance records written by Teacher/HOD using WriteBatch */
    public static final String COL_ATTENDANCE_RECORDS = "attendanceRecords";

    /** Student leave requests */
    public static final String COL_LEAVE_REQUESTS = "leaveRequests";

    /** Tournament documents */
    public static final String COL_TOURNAMENTS = "tournaments";

    /** Tournament registration subcollection path under a tournament doc */
    public static final String COL_TEAMS = "teams";

    /** PT Admin equipment inventory */
    public static final String COL_INVENTORY = "inventory";

    /** Student equipment borrow + return requests */
    public static final String COL_BORROW_REQUESTS = "borrowRequests";

    /** Feedback / complaints from students */
    public static final String COL_FEEDBACK = "feedback";

    /** Audit log of honor score changes */
    public static final String COL_HONOR_EVENTS = "honorEvents";

    /** Tournament invitation dispatches by teachers */
    public static final String COL_TOURNAMENT_INVITATIONS = "tournamentInvitations";

    public static final String COL_TIMETABLE = "timetable";

    /** Internal marks */
    public static final String COL_INTERNALS = "internals";

    /** Reconsideration requests for internal marks */
    public static final String COL_RECONSIDERATIONS = "reconsiderations";

    /** HOD Subject Assignments */
    public static final String COL_SUBJECT_ASSIGNMENTS = "subjectAssignments";


    // ── Firestore field names ─────────────────────────────────────────────────

    /** Inventory available stock count */
    public static final String FIELD_REMAINING = "remaining";

    /** Borrow/return request status values */
    public static final String STATUS_PENDING           = "pending";
    public static final String STATUS_ACTIVE            = "active";
    public static final String STATUS_RETURN_REQUESTED  = "return_requested";
    public static final String STATUS_RETURNED          = "returned";
    public static final String STATUS_APPROVED          = "approved";
    public static final String STATUS_REJECTED          = "rejected";

    /** Attendance status values */
    public static final String ATTENDANCE_PRESENT = "P";
    public static final String ATTENDANCE_ABSENT  = "A";
    public static final String ATTENDANCE_LEAVE   = "L";

    /** User role values */
    public static final String ROLE_STUDENT  = "Student";
    public static final String ROLE_TEACHER  = "Teacher";
    public static final String ROLE_HOD      = "HOD";
    public static final String ROLE_PT_ADMIN = "PT Admin";
    public static final String ROLE_PRINCIPAL= "Principal";
}
