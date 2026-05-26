package com.procollegia.utils;

import java.util.regex.Pattern;

/**
 * Centralised, pure-Java input validation utility for ProCollegia.
 *
 * All methods are static and have zero Android/Firebase dependencies,
 * making them fully testable with plain JUnit without an emulator.
 */
public class InputValidator {

    // RFC-5322 simplified e-mail pattern
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Exactly 10 digits
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9]{10}$");

    // ──────────────────────────────────────────────────────────────────────
    // E-mail
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Returns true if {@code email} is non-null, non-blank, and matches the
     * basic e-mail format (local@domain.tld).
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Password
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Returns true if {@code password} is at least 6 characters long.
     * Firebase Auth requires a minimum of 6 characters.
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    // ──────────────────────────────────────────────────────────────────────
    // Phone
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Returns true if {@code phone} is exactly 10 numeric digits.
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return false;
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Institution login code
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Returns true if {@code enteredCode} exactly matches {@code expectedCode}.
     * Both values are trimmed before comparison.
     */
    public static boolean isValidLoginCode(String enteredCode, String expectedCode) {
        if (enteredCode == null || expectedCode == null) return false;
        return enteredCode.trim().equals(expectedCode.trim());
    }

    // ──────────────────────────────────────────────────────────────────────
    // UUCMS ID (mandatory for Students)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Returns true if {@code uucmsId} is non-null and non-blank.
     * Required for all Student role registrations.
     */
    public static boolean isValidUucmsId(String uucmsId) {
        return uucmsId != null && !uucmsId.trim().isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Composite: full registration personal-tab check
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Returns true if all four personal-tab fields are non-empty AND
     * the email/phone/password individually pass their own checks.
     */
    public static boolean arePersonalDetailsValid(String name, String phone,
                                                   String email, String password) {
        if (name == null || name.trim().isEmpty()) return false;
        return isValidPhone(phone) && isValidEmail(email) && isValidPassword(password);
    }
}
