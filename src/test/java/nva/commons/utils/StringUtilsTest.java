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

    @Test
    @DisplayName("isEmpty should return false for a non empty string")
    public void isEmptyReturnsFalseForANonEmptyString() {
        String nonEmptyString = "abc";
        assertThat(StringUtils.isEmpty(nonEmptyString), is(equalTo(false)));
    }

    @Test
    @DisplayName("replaceWhitespacesWithSpace replaces all white spaces with space.")
    public void replaceWhiteSpacesWithSpaceReturnsStringWithspaces() {
        String inputString = "This is a\ntest\tfor\n\rwhitespaces";
        String expectedString = "This is a test for  whitespaces";
        String actual = StringUtils.replaceWhiteSpacesWithSpace(inputString);
        assertThat(actual, is(equalTo(expectedString)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("isEmpty should return true for empty string")
    public void stringUtilReturnsTrueForANullString(String input) {
        assertThat(StringUtils.isEmpty(null), is(equalTo(true)));
    }
}
