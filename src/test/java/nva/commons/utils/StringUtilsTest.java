package nva.commons.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class StringUtilsTest {

    private static final String EXPECTED_OUTPUT = "The quick brown fox jumps over the lazy dog";

    @Test
    @DisplayName("removeMultipleWhitespaces preserves single whitespaces")
    void removeMulitpleWhitespacesPreserversSingleWhitespacess() {
        String input = EXPECTED_OUTPUT;
        String output = StringUtils.removeMultipleWhiteSpaces(input);
        ;
        assertThat(output, is(equalTo(EXPECTED_OUTPUT)));
    }

    @Test
    @DisplayName("removeMultipleWhitespaces removes double spaces")
    void removeMultipleWhitespacesRemovesDoubleSpaces() {
        String doubleSpaces = "The  quick  brown  fox  jumps over the lazy dog";
        String output = StringUtils.removeMultipleWhiteSpaces(doubleSpaces);
        ;
        assertThat(output, is(equalTo(EXPECTED_OUTPUT)));
    }

    @Test
    @DisplayName("removeMultipleWhitespaces removes double spaces")
    void removeMultipleWhitespacesRemovesCombinationOfSpaces() {
        String combinationOfSpaces = "The \n quick\n brown\n fox  jumps over the lazy dog";
        String output = StringUtils.removeMultipleWhiteSpaces(combinationOfSpaces);

        assertThat(output, is(equalTo(EXPECTED_OUTPUT)));
    }

    @Test
    @DisplayName("removeWhitespaces removes whitespaces")
    void removeWhiteSpacesRemovesWhiteSpaces() {
        String combinationOfSpaces = "The \n quick\n brown\n fox  jumps over the lazy dog";
        String expected = "Thequickbrownfoxjumpsoverthelazydog";
        String output = StringUtils.removeWhiteSpaces(combinationOfSpaces);
        assertThat(output, is(equalTo(expected)));
    }

    @Test
    @DisplayName("StringUtils should have a method for checking if a string is empty (null or blank)")
    void stringUtilsHasAMethodForCheckingIfAStringIsEmpty() {
        boolean result = StringUtils.isEmpty(null);
    }

    @ParameterizedTest(name = "[{index}] isEmpty should return false for a non empty string: \"{0}\"")
    @ValueSource(strings = {"abc", "\t", "\n", " "})
    public void isEmptyReturnsFalseForANonEmptyString(String input) {
        assertThat(StringUtils.isEmpty(input), is(equalTo(false)));
    }

    @ParameterizedTest(name = "[{index}]  {displayName} with input: \"{0}\"")
    @ValueSource(strings = {"abc", " a", "a "})
    public void isBlankReturnsFalseForANonBlankString(String input) {
        assertThat(StringUtils.isBlank(input), is(equalTo(false)));
    }

    @Test
    @DisplayName("replaceWhitespacesWithSpace replaces all white spaces with space")
    public void replaceWhiteSpacesWithSpaceReturnsStringWithspaces() {
        String inputString = "This is a\ntest\tfor\n\rwhitespaces";
        String expectedString = "This is a test for  whitespaces";
        String actual = StringUtils.replaceWhiteSpacesWithSpace(inputString);
        assertThat(actual, is(equalTo(expectedString)));
    }

    @ParameterizedTest(name = "[{index}]  {displayName} with input: \"{0}\"")
    @NullAndEmptySource
    @DisplayName("isEmpty should return true for empty string")
    public void stringUtilReturnsTrueForANullString(String input) {
        assertThat(StringUtils.isEmpty(input), is(equalTo(true)));
    }

    @ParameterizedTest(name = "[{index}]  {displayName} with input: \"{0}\"")
    @ValueSource(strings = {" ", "\t", "\n", "a"})
    @DisplayName("isNotNullOrEmpty should return true where string is neither null or empty")
    public void stringUtilIsNotNullOrEmpty(String input) {
        assertThat(StringUtils.isNotNullOrEmpty(input), is(equalTo(true)));
    }

    @ParameterizedTest(name = "[{index}]  {displayName} with input: \"{0}\"")
    @NullAndEmptySource
    @DisplayName("isNullOrEmpty should return true where string is null or empty.")
    public void stringUtilIsNullOrEmpty(String input) {
        assertThat(StringUtils.isNullOrEmpty(input), is(equalTo(true)));
    }

    @ParameterizedTest(name = "[{index}]  {displayName} with input: \"{0}\"")
    @ValueSource(strings = {"a", " a", "a "})
    @DisplayName("isNotNullOrBlank should return true where string is neither null or blank.")
    public void stringUtilIsNotNullOrBlank(String input) {
        assertThat(StringUtils.isNotNullOrBlank(input), is(equalTo(true)));
    }

    @ParameterizedTest(name = "[{index}]  {displayName} with input: \"{0}\"")
    @NullAndEmptySource
    @DisplayName("isNullOrEmpty should return true where string is null or empty.")
    public void stringUtilIsNullOrBlank(String input) {
        assertThat(StringUtils.isNullOrBlank(input), is(equalTo(true)));
    }
}
