package no.unit.nva.testutils;

final class IssnGenerator {

    public static final String SYMBOL_FOR_NUMBER_TEN = "X";
    private static final int MIDDLE_INDEX_OF_ISSN_STRING = 4;

    private IssnGenerator() {

    }

    public static String randomInvalidIssn() {
        return createInvalidIssn(random7DigitNumber());
    }

    public static String randomIssn() {
        return createValidIssn(random7DigitNumber());
    }

    private static String random7DigitNumber() {
        return String.valueOf((int) (Math.random() * 9_000_000) + 1_000_000);
    }

    private static String createInvalidIssn(String issnMissingChecksum) {
        int issnDigitsSum = calculateIssnDigitSum(issnMissingChecksum);
        int wrongSum = issnDigitsSum - 1;
        String issnWithChecksum = createIssnWithCheckSum(issnMissingChecksum, wrongSum);
        return formatIssnString(issnWithChecksum);
    }

    private static String createValidIssn(String issnMissingChecksum) {
        int issnDigitsSum = calculateIssnDigitSum(issnMissingChecksum);
        String issnWithChecksum = createIssnWithCheckSum(issnMissingChecksum, issnDigitsSum);
        return formatIssnString(issnWithChecksum);
    }

    private static String formatIssnString(String issnWithChecksum) {
        return issnWithChecksum.substring(0, MIDDLE_INDEX_OF_ISSN_STRING)
               + "-"
               + issnWithChecksum.substring(MIDDLE_INDEX_OF_ISSN_STRING);
    }

    private static int calculateIssnDigitSum(String issnMissingChecksum) {
        int issnDigitsSum = 0;
        for (int i = 0; i < issnMissingChecksum.length(); i++) {
            int number = Integer.parseInt(Character.toString(issnMissingChecksum.charAt(i)));
            issnDigitsSum += (8 - i) * number;
        }
        return issnDigitsSum;
    }

    private static String createIssnWithCheckSum(String issnMissingChecksum, int totalSum) {
        int mod11 = totalSum % 11;
        int checksum = mod11 == 0 ? 0 : 11 - mod11;
        return checksum == 10
                   ? issnMissingChecksum + SYMBOL_FOR_NUMBER_TEN
                   : issnMissingChecksum + checksum;
    }
}
