package nva.commons.utils;

import static java.util.Objects.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.internal.util.StringUtil;

public class StringUtilsTest {

    private static final String EXPECTED_OUTPUT = "The quick brown fox jumps over the lazy dog";
    private static final String PARAMETERIZED_TEST_NAME_FORMAT = "[{index}]  {displayName} with input: \"{0}\"";

    @Test
    @DisplayName("removeMultipleWhitespaces preserves single whitespaces")
    public void removeMulitpleWhitespacesPreserversSingleWhitespacess() {
        String input = EXPECTED_OUTPUT;
        String output = StringUtils.removeMultipleWhiteSpaces(input);
        ;
        assertThat(output, is(equalTo(EXPECTED_OUTPUT)));
    }

    @Test
    @DisplayName("removeMultipleWhitespaces removes double spaces")
    public void removeMultipleWhitespacesRemovesDoubleSpaces() {
        String doubleSpaces = "The  quick  brown  fox  jumps over the lazy dog";
        String output = StringUtils.removeMultipleWhiteSpaces(doubleSpaces);
        ;
        assertThat(output, is(equalTo(EXPECTED_OUTPUT)));
    }

    @Test
    @DisplayName("removeMultipleWhitespaces removes double spaces")
    public void removeMultipleWhitespacesRemovesCombinationOfSpaces() {
        String combinationOfSpaces = "The \n quick\n brown\n fox  jumps over the lazy dog";
        String output = StringUtils.removeMultipleWhiteSpaces(combinationOfSpaces);

        assertThat(output, is(equalTo(EXPECTED_OUTPUT)));
    }

    @Test
    @DisplayName("removeWhitespaces removes whitespaces")
    public void removeWhiteSpacesRemovesWhiteSpaces() {
        String combinationOfSpaces = "The \n quick\n brown\n fox  jumps over the lazy dog";
        String expected = "Thequickbrownfoxjumpsoverthelazydog";
        String output = StringUtils.removeWhiteSpaces(combinationOfSpaces);
        assertThat(output, is(equalTo(expected)));
    }

    @Test
    @DisplayName("StringUtils should have a method for checking if a string is empty (null or blank)")
    public void stringUtilsHasAMethodForCheckingIfAStringIsEmpty() {
        boolean result = StringUtils.isEmpty(null);
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_NAME_FORMAT)
    @ValueSource(strings = {"abc", "\t", "\n", " "})
    public void isEmptyReturnsFalseForANonEmptyString(String input) {
        assertThat(StringUtils.isEmpty(input), is(equalTo(false)));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_NAME_FORMAT)
    @ValueSource(strings = {"abc", " a", "a "})
    public void isBlankReturnsFalseForANonBlankString(String input) {
        assertThat(StringUtils.isBlank(input), is(equalTo(false)));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_NAME_FORMAT)
    @ValueSource(strings = {"abc", " a", "a "})
    @DisplayName("isNotBlank should return true where string is neither null or blank.")
    public void stringUtilIsNotBlank(String input) {
        assertThat(StringUtils.isNotBlank(input), is(equalTo(true)));
    }

    @Test
    @DisplayName("replaceWhitespacesWithSpace replaces all white spaces with space")
    public void replaceWhiteSpacesWithSpaceReturnsStringWithspaces() {
        String inputString = "This is a\ntest\tfor\n\rwhitespaces";
        String expectedString = "This is a test for  whitespaces";
        String actual = StringUtils.replaceWhiteSpacesWithSpace(inputString);
        assertThat(actual, is(equalTo(expectedString)));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_NAME_FORMAT)
    @NullAndEmptySource
    @DisplayName("isEmpty should return true for empty string")
    public void stringUtilReturnsTrueForANullString(String input) {
        assertThat(StringUtils.isEmpty(input), is(equalTo(true)));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_NAME_FORMAT)
    @ValueSource(strings = {" ", "\t", "\n", "a"})
    @DisplayName("isNotNullOrEmpty should return true where string is neither null or empty")
    @Deprecated(forRemoval = true)
    public void stringUtilIsNotNullOrEmpty(String input) {
        assertThat(StringUtils.isNotNullOrEmpty(input), is(equalTo(true)));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_NAME_FORMAT)
    @ValueSource(strings = {" ", "\t", "\n", "a"})
    @DisplayName("isNotEmpty should return true where string is neither null or empty")
    public void stringUtilIsNotEmpty(String input) {
        assertThat(StringUtils.isNotEmpty(input), is(equalTo(true)));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_NAME_FORMAT)
    @NullAndEmptySource
    @DisplayName("isNullOrEmpty should return true where string is null or empty.")
    @Deprecated(forRemoval = true)
    public void stringUtilIsNullOrEmpty(String input) {
        assertThat(StringUtils.isNullOrEmpty(input), is(equalTo(true)));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_NAME_FORMAT)
    @NullAndEmptySource
    @DisplayName("isEmpty should return true where string is null or empty.")
    public void stringUtilIsEmpty(String input) {
        assertThat(StringUtils.isEmpty(input), is(equalTo(true)));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_NAME_FORMAT)
    @ValueSource(strings = {"a", " a", "a "})
    @DisplayName("isNotNullOrBlank should return true where string is neither null or blank.")
    @Deprecated(forRemoval = true)
    public void stringUtilIsNotNullOrBlank(String input) {
        assertThat(StringUtils.isNotNullOrBlank(input), is(equalTo(true)));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_NAME_FORMAT)
    @ValueSource(strings = {" ", "\t", "\n", ""})
    @NullAndEmptySource
    @DisplayName("isNullOrBlank should return true where string is null or blank.")
    @Deprecated(forRemoval = true)
    public void stringUtilIsNullOrBlank(String input) {
        assertThat(StringUtils.isNullOrBlank(input), is(equalTo(true)));
    }
}
