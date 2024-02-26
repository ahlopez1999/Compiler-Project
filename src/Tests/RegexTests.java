package Tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import Main.Regex;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. Test structure for steps 1 & 2 are
 * provided, you must create this yourself for step 3.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),

                //matching tests
                Arguments.of("All caps email", "SCREAMINGEMAIL@gmail.com", true),
                Arguments.of("All numbers email", "12345678@gmail.com", true),
                Arguments.of("AOL domain", "thelegend27@aol.com", true),
                Arguments.of("Numbers in domain", "thelegend27@123.com", true),
                Arguments.of("Two character domain", "thelegend27@gmail.ru", true),

                //non-matching tests
                Arguments.of("No username", "@gmail.com", false),
                Arguments.of("One character domain", "thelegend27@gmail.c", false),
                Arguments.of("Numbers in domain (after dot)", "thelegend27@gmail.123", false),
                Arguments.of("Symbols in domain", "thelegend27@#$!.com", false),
                Arguments.of("Extra dot", "genericemail@gmail..com", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testEvenStringsRegex(String test, String input, boolean success) {
        test(input, Regex.EVEN_STRINGS, success);
    }

    public static Stream<Arguments> testEvenStringsRegex() {
        return Stream.of(
                //what has ten letters and starts with gas?
                Arguments.of("10 Characters", "automobile", true),
                Arguments.of("14 Characters", "i<3pancakes10!", true),
                Arguments.of("6 Characters", "6chars", false),
                Arguments.of("13 Characters", "i<3pancakes9!", false),

                //matching tests
                Arguments.of("10 Characters all the same", "aaaaaaaaaa", true),
                Arguments.of("10 Characters all symbols", "!@#$%^&*()", true),
                Arguments.of("20 Characters", "awesome programmers1", true),
                Arguments.of("18 Characters", "Lord Peter Dobbins", true),
                Arguments.of("16 Characters", "COP 4020 is dope", true),

                //non-matching tests
                Arguments.of("No Characters", "", false),
                Arguments.of("1 Character", "E", false),
                Arguments.of("21 Characters", "abcdefghijklmnopqrstu", false),
                Arguments.of("19 Characters", " 19 characters long", false),
                Arguments.of("New line character", "\n", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testIntegerListRegex(String test, String input, boolean success) {
        test(input, Regex.INTEGER_LIST, success);
    }

    public static Stream<Arguments> testIntegerListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "[1]", true),
                Arguments.of("Multiple Elements", "[1,2,3]", true),
                Arguments.of("Missing Brackets", "1,2,3", false),
                Arguments.of("Missing Commas", "[1 2 3]", false),

                //matching tests
                Arguments.of("No elements", "[]", true),
                Arguments.of("Commas w/spaces", "[1, 2, 3]", true),
                Arguments.of("Mixed Spacing", "[1,2, 3]", true),
                Arguments.of("Even sized list", "[1,2,3,4]", true),
                Arguments.of("Multi-digit numbers", "[10,20,30]", true),

                //non-matching tests
                Arguments.of("Trailing comma", "[1,2,3,]", false),
                Arguments.of("Missing Comma", "[1 2,3]", false),
                Arguments.of("Extra commas", "[1,2,,3]", false),
                Arguments.of("Extra Spaces", "[1, 2,  3]", false),
                Arguments.of("Single comma", "[,]", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testNumberRegex(String test, String input, boolean success) {
        test(input, Regex.NUMBER, success);
    }

    public static Stream<Arguments> testNumberRegex() {
        return Stream.of(
                //matching tests
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "123", true),
                Arguments.of("Single Digit decimal", "1.2", true),
                Arguments.of("Multi Digit decimal", "12.34", true),
                Arguments.of("Number w/Positive", "+123", true),
                Arguments.of("Number w/Negative", "-123", true),
                Arguments.of("Decimal w/Positive", "+1.2", true),
                Arguments.of("Decimal w/Negative", "-1.2", true),

                //non-matching tests
                Arguments.of("Trailing decimal", "1.", false),
                Arguments.of("Leading decimal", ".1", false),
                Arguments.of("Multiple decimals", "1..2", false),
                Arguments.of("Multiple signs", "+-123", false),
                Arguments.of("Multiple decimals (dispersed)", "1.2.3", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                //matching tests
                Arguments.of("Empty string", "\"\"", true),
                Arguments.of("Sentence", "\"This is a sentence.\"", true),
                Arguments.of("Escape characters", "\"1\\t2\"", true),
                Arguments.of("Symbols", "\"!@#$%\"", true),
                Arguments.of("Some more escape characters", "\"\\nescape\\\\escape\\'escape\\bescape\"", true),

                //non-matching tests
                Arguments.of("Unterminated string", "\"unterminated", false),
                Arguments.of("Invalid escape characters", "\"invalid\\escape\"", false),
                Arguments.of("Missing quote", "whoops\"", false),
                Arguments.of("No quotes", "extra whoops", false),
                Arguments.of("Valid escape w/invalid escape", "\"right\\b wrong\\s\"", false)
        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
