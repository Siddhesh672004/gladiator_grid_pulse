package com.gridpulse.core.platform.validation;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

// rules shared by more than one form, used by the request records in dto/request
// twin on the angular side: validation.ts (keep the two the same)
public final class ValidationRules {

    public static final int EMAIL_MAX = 100;
    public static final String EMAIL_NO_SPACE_PATTERN = "^\\S*$";
    public static final String EMAIL_ONE_AT_PATTERN = "^[^@]*@[^@]*$";
    public static final String EMAIL_DOMAIN_PATTERN = "^.*@.+\\..+$";
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    public static final String EMAIL_REQUIRED = "Email is required";
    public static final String EMAIL_TOO_LONG = "Email must be at most " + EMAIL_MAX + " characters";
    public static final String EMAIL_HAS_SPACE = "Email cannot have spaces";
    public static final String EMAIL_ONE_AT = "Email must have exactly one @";
    public static final String EMAIL_NO_DOMAIN = "Email must have a domain after @, like gmail.com";
    public static final String EMAIL_INVALID = "Please enter a valid email";
    public static final String EMAIL_TAKEN = "Email is already registered";

    // admins may leave the mobile number empty
    public static final String MOBILE_DIGITS_PATTERN = "^[0-9]*$";
    public static final String MOBILE_LENGTH_PATTERN = "^.{10}$";
    public static final String MOBILE_START_PATTERN = "^[6-9].*$";
    public static final String MOBILE_REQUIRED = "Mobile number is required";
    public static final String MOBILE_DIGITS_ONLY = "Mobile number can only have digits";
    public static final String MOBILE_WRONG_LENGTH = "Mobile number must be exactly 10 digits";
    public static final String MOBILE_START = "Mobile number must start with 6, 7, 8 or 9";
    public static final String MOBILE_TAKEN = "Mobile number is already registered";

    public static final int USERNAME_MIN = 3;
    public static final int USERNAME_MAX = 50;
    public static final String USERNAME_PATTERN = "^[A-Za-z ]+$";
    public static final String USERNAME_ONE_SPACE_PATTERN = "^[^ ]+( [^ ]+)*$";
    public static final String USERNAME_REQUIRED = "Username is required";
    public static final String USERNAME_TOO_SHORT = "Username must be at least " + USERNAME_MIN + " characters";
    public static final String USERNAME_TOO_LONG = "Username must be at most " + USERNAME_MAX + " characters";
    public static final String USERNAME_INVALID = "Username can only have letters and spaces";
    public static final String USERNAME_DOUBLE_SPACE = "Username cannot have two spaces in a row";

    public static final String CODE_DIGITS_PATTERN = "^[0-9]*$";
    public static final String CODE_LENGTH_PATTERN = "^.{6}$";
    public static final String CODE_REQUIRED = "Code is required";
    public static final String CODE_DIGITS_ONLY = "Code can only have digits";
    public static final String CODE_INVALID = "Code must be exactly 6 digits";

    // DL + 4 digits, licenses saved before this rule stay as they are
    public static final int LICENSE_LENGTH = 6;
    public static final String LICENSE_START_PATTERN = "^DL.*$";
    public static final String LICENSE_DIGITS_PATTERN = "^DL[0-9]{4}$";
    public static final String LICENSE_REQUIRED = "License number is required";
    public static final String LICENSE_NO_DL = "License number must start with DL";
    public static final String LICENSE_DIGITS = "License number must have exactly 4 digits after DL";
    public static final String LICENSE_TAKEN = "Driver license number is already registered to another user";

    public static final int MIN_AGE = 18;
    public static final LocalDate DOB_EARLIEST = LocalDate.of(1920, 1, 1);
    public static final String DOB_REQUIRED = "Date of birth is required";
    public static final String DOB_IN_FUTURE = "Date of birth must be in the past";
    public static final String DOB_NOT_REAL = "Please enter a real date of birth";
    public static final String TOO_YOUNG = "You must be at least " + MIN_AGE + " years old";

    public static final String HAS_LETTER_PATTERN = "^.*[A-Za-z].*$";
    public static final String TRACK_NAME_NO_LETTER = "Track Name must have a letter";
    public static final String LOCATION_NO_LETTER = "Location must have a letter";
    public static final String COMPETITION_NAME_NO_LETTER = "Competition Name must have a letter";

    public static final int PRICE_MAX = 100000;

    // when one box breaks two patterns, the earlier one in this list is shown (rank in GlobalExceptionHandler)
    public static final List<String> PATTERN_ORDER = List.of(USERNAME_INVALID, USERNAME_DOUBLE_SPACE,
            EMAIL_HAS_SPACE, EMAIL_ONE_AT, EMAIL_NO_DOMAIN, EMAIL_INVALID,
            MOBILE_DIGITS_ONLY, MOBILE_WRONG_LENGTH, MOBILE_START, CODE_DIGITS_ONLY, CODE_INVALID,
            LICENSE_NO_DL, LICENSE_DIGITS, TRACK_NAME_NO_LETTER, LOCATION_NO_LETTER, COMPETITION_NAME_NO_LETTER);

    // a typed text without spaces around it, blank = null (so @NotBlank says "required"). never used for passwords
    public static String clean(String text) {
        // only spaces = nothing typed
        return Optional.ofNullable(text)
                .filter(typed -> !typed.isBlank())
                .map(String::trim)
                .orElse(null);
    }

    public static String cleanUpper(String text) {
        // nothing typed stays null
        return Optional.ofNullable(clean(text))
                .map(String::toUpperCase)
                .orElse(null);
    }

    // dates are checked in India time wherever the server runs (validation.ts does the same)
    public static final ZoneId INDIA_TIME = ZoneId.of("Asia/Kolkata");

    public static LocalDate today() {
        return LocalDate.now(INDIA_TIME);
    }

    // the 18th birthday itself counts
    public static boolean isAdult(LocalDate dateOfBirth) {
        return !dateOfBirth.plusYears(MIN_AGE).isAfter(today());
    }

    // one rule of a box: how to see that it is broken + the message for it
    private record Rule<T>(Predicate<T> broken, String message) {
    }

    // today is read on every check, never kept
    private static final List<Rule<LocalDate>> DOB_RULES = List.of(
            new Rule<LocalDate>(date -> !date.isBefore(today()), DOB_IN_FUTURE),
            new Rule<LocalDate>(date -> date.isBefore(DOB_EARLIEST), DOB_NOT_REAL),
            new Rule<LocalDate>(date -> !isAdult(date), TOO_YOUNG));

    // the first broken rule of a date of birth, "" = fine (same order as adult in validation.ts)
    public static String dateOfBirthProblem(LocalDate dateOfBirth) {
        return firstProblem(DOB_RULES, dateOfBirth);
    }

    public static final List<String> PICTURE_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    public static final int PICTURE_MAX_MB = 5;
    public static final long PICTURE_MAX_BYTES = PICTURE_MAX_MB * 1024L * 1024L;
    public static final String PICTURE_REQUIRED = "Please choose a picture";
    public static final String PICTURE_TYPE = "Only JPG, PNG or WebP pictures can be uploaded";
    public static final String PICTURE_TOO_BIG = "A picture must be at most " + PICTURE_MAX_MB + " MB";
    public static final String PICTURE_UNREADABLE = "This picture could not be read. Please choose another JPG, PNG or WebP file";
    public static final String PICTURE_NOT_OURS = "Pictures must be uploaded through GridPulse";
    public static final int TRACK_PICTURES_MIN = 1;
    public static final int TRACK_PICTURES_MAX = 5;
    public static final String TRACK_PICTURES_REQUIRED = "Please add at least " + TRACK_PICTURES_MIN + " picture of the track";
    public static final String TRACK_PICTURES_TOO_MANY = "A track can have at most " + TRACK_PICTURES_MAX + " pictures";

    // only for NEW passwords (sign up, reset, add admin), login never checks these
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 64;
    // BCrypt hashes 72 bytes at most, a letter outside English is 2 or 3 bytes (StrongPasswordValidator)
    public static final int PASSWORD_MAX_BYTES = 72;
    public static final String PASSWORD_TOO_MANY_BYTES = "Password is too long, please use a shorter one";

    // the password rules, checked from the top
    private static final List<Rule<String>> PASSWORD_RULES = List.of(
            new Rule<String>(password -> password.length() < PASSWORD_MIN,
                    "Password must be at least " + PASSWORD_MIN + " characters"),
            new Rule<String>(password -> password.length() > PASSWORD_MAX,
                    "Password must be at most " + PASSWORD_MAX + " characters"),
            new Rule<String>(password -> password.contains(" "),
                    "Password cannot have spaces"),
            new Rule<String>(password -> !password.matches(".*[A-Z].*"),
                    "Password must have at least one uppercase letter"),
            new Rule<String>(password -> !password.matches(".*[a-z].*"),
                    "Password must have at least one lowercase letter"),
            new Rule<String>(password -> !password.matches(".*[0-9].*"),
                    "Password must have at least one digit"),
            new Rule<String>(password -> !password.matches(".*[^A-Za-z0-9 ].*"),
                    "Password must have at least one special character"));

    // the first broken rule, "" = the password is fine
    public static String passwordProblem(String password) {
        return firstProblem(PASSWORD_RULES, password);
    }

    private static <T> String firstProblem(List<Rule<T>> rules, T value) {
        // rules are tried in list order, the first broken one is the message
        return rules.stream()
                .filter(rule -> rule.broken().test(value))
                .map(Rule::message)
                .findFirst()
                .orElse("");
    }

    private ValidationRules() {
    }
}
