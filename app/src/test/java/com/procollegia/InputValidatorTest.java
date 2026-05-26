package com.procollegia;

import com.procollegia.utils.InputValidator;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link InputValidator}.
 *
 * These tests verify that ALL validation rules behave exactly as intended,
 * covering both the happy path and every edge-case / boundary condition.
 *
 * Run with:  ./gradlew testDebugUnitTest
 */
public class InputValidatorTest {

    // ─────────────────────────────────────────────────────────────────────────
    // E-mail
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void validEmail_isAccepted() {
        assertTrue(InputValidator.isValidEmail("tejas@procollegia.com"));
    }

    @Test
    public void validEmail_withSubdomain_isAccepted() {
        assertTrue(InputValidator.isValidEmail("user@mail.university.edu"));
    }

    @Test
    public void emptyEmail_isRejected() {
        assertFalse(InputValidator.isValidEmail(""));
    }

    @Test
    public void nullEmail_isRejected() {
        assertFalse(InputValidator.isValidEmail(null));
    }

    @Test
    public void emailWithoutAtSign_isRejected() {
        assertFalse(InputValidator.isValidEmail("invalidEmail.com"));
    }

    @Test
    public void emailWithoutDomain_isRejected() {
        assertFalse(InputValidator.isValidEmail("user@"));
    }

    @Test
    public void emailWithSpaces_isRejected() {
        // whitespace-only or interior space → no match
        assertFalse(InputValidator.isValidEmail("user @procollegia.com"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Password
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void passwordWithSixChars_isAccepted() {
        assertTrue(InputValidator.isValidPassword("abcdef"));
    }

    @Test
    public void passwordWithMoreThanSixChars_isAccepted() {
        assertTrue(InputValidator.isValidPassword("SuperSecure@123"));
    }

    @Test
    public void passwordWithFiveChars_isRejected() {
        assertFalse(InputValidator.isValidPassword("abc12"));
    }

    @Test
    public void emptyPassword_isRejected() {
        assertFalse(InputValidator.isValidPassword(""));
    }

    @Test
    public void nullPassword_isRejected() {
        assertFalse(InputValidator.isValidPassword(null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phone
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void validTenDigitPhone_isAccepted() {
        assertTrue(InputValidator.isValidPhone("9876543210"));
    }

    @Test
    public void phoneWithElevenDigits_isRejected() {
        assertFalse(InputValidator.isValidPhone("98765432100"));
    }

    @Test
    public void phoneWithNineDigits_isRejected() {
        assertFalse(InputValidator.isValidPhone("987654321"));
    }

    @Test
    public void phoneWithLetters_isRejected() {
        assertFalse(InputValidator.isValidPhone("98765ABCDE"));
    }

    @Test
    public void emptyPhone_isRejected() {
        assertFalse(InputValidator.isValidPhone(""));
    }

    @Test
    public void nullPhone_isRejected() {
        assertFalse(InputValidator.isValidPhone(null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Institution Login Code
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void correctLoginCode_isAccepted() {
        assertTrue(InputValidator.isValidLoginCode("Login", "Login"));
    }

    @Test
    public void loginCodeWithLeadingSpaces_isAccepted() {
        // trim() must handle whitespace on both sides
        assertTrue(InputValidator.isValidLoginCode("  Login  ", "Login"));
    }

    @Test
    public void wrongLoginCode_isRejected() {
        assertFalse(InputValidator.isValidLoginCode("WrongCode", "Login"));
    }

    @Test
    public void emptyLoginCode_isRejected() {
        assertFalse(InputValidator.isValidLoginCode("", "Login"));
    }

    @Test
    public void nullLoginCode_isRejected() {
        assertFalse(InputValidator.isValidLoginCode(null, "Login"));
    }

    @Test
    public void codeIsCaseSensitive() {
        // "login" ≠ "Login" — the institution code is case-sensitive
        assertFalse(InputValidator.isValidLoginCode("login", "Login"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UUCMS ID
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void nonEmptyUucms_isAccepted() {
        assertTrue(InputValidator.isValidUucmsId("UUCMS2024001"));
    }

    @Test
    public void emptyUucms_isRejected() {
        assertFalse(InputValidator.isValidUucmsId(""));
    }

    @Test
    public void blankUucms_isRejected() {
        assertFalse(InputValidator.isValidUucmsId("   "));
    }

    @Test
    public void nullUucms_isRejected() {
        assertFalse(InputValidator.isValidUucmsId(null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Composite: arePersonalDetailsValid
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void allValidPersonalDetails_returnsTrue() {
        assertTrue(InputValidator.arePersonalDetailsValid(
                "Tejas", "9876543210", "tejas@college.edu", "secure123"));
    }

    @Test
    public void emptyName_returnsFalse() {
        assertFalse(InputValidator.arePersonalDetailsValid(
                "", "9876543210", "tejas@college.edu", "secure123"));
    }

    @Test
    public void invalidEmail_inPersonalDetails_returnsFalse() {
        assertFalse(InputValidator.arePersonalDetailsValid(
                "Tejas", "9876543210", "not-an-email", "secure123"));
    }

    @Test
    public void shortPassword_inPersonalDetails_returnsFalse() {
        assertFalse(InputValidator.arePersonalDetailsValid(
                "Tejas", "9876543210", "tejas@college.edu", "abc"));
    }

    @Test
    public void invalidPhone_inPersonalDetails_returnsFalse() {
        assertFalse(InputValidator.arePersonalDetailsValid(
                "Tejas", "12345", "tejas@college.edu", "secure123"));
    }
}
