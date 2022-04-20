package no.unit.nva.testutils;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Random;
import net.datafaker.Faker;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.JacocoGenerated;
import org.apache.commons.lang3.RandomStringUtils;

@JacocoGenerated
public final class RandomDataGenerator {

    public static final int MIN_RANDOM_STRING_LENGTH = 10;
    public static final int MAX_RANDOM_STRING_LENGTH = 20;
    public static final Random RANDOM = new Random();
    public static final Faker FAKER = new Faker();
    public static final Instant BEGINNING_OF_TIME =
        LocalDateTime.of(1971, Month.JANUARY, 2, 0, 0).toInstant(ZoneOffset.UTC);
    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;
    private static final Instant END_OF_TIME = Instant.now();
    private static final int ARBITRARY_FIELDS_NUMBER = 5;
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static final ZoneOffset CURRENT_ZONE_OFFSET = ZONE_ID.getRules().getOffset(Instant.now());

    private RandomDataGenerator() {

    }

    public static String randomString() {
        return RandomStringUtils.randomAlphanumeric(MIN_RANDOM_STRING_LENGTH, MAX_RANDOM_STRING_LENGTH);
    }

    public static Integer randomInteger() {
        return randomInteger(Integer.MAX_VALUE);
    }

    public static Integer randomInteger(int bound) {
        return RANDOM.nextInt(bound);
    }

    public static URI randomUri() {
        return URI.create("https://www.example.com/" + randomString());
    }

    public static URI randomDoi() {
        return URI.create("https://doi.org/10.1000/" + randomInteger(10_000));
    }

    public static String randomIsbn10() {
        return FAKER.code().isbn10();
    }

    public static String randomIsbn13() {
        return FAKER.code().isbn13();
    }

    public static boolean randomBoolean() {
        return randomElement(true, false);
    }

    public static <T> T randomElement(T... elements) {
        return elements[RANDOM.nextInt(elements.length)];
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T randomElement(Collection<T> elements) {
        Object element = randomElement(elements.toArray());
        return (T) element;
    }

    public static LocalDateTime randomLocalDateTime() {
        return LocalDateTime.ofInstant(randomInstant(), ZONE_ID);
    }

    public static LocalDateTime randomLocalDateTime(LocalDateTime after) {
        var afterAsInstant = Instant.from(after.toInstant(CURRENT_ZONE_OFFSET));
        return LocalDateTime.ofInstant(randomInstant(afterAsInstant), ZONE_ID);
    }

    public static LocalDate randomLocalDate() {
        return LocalDate.from(randomLocalDateTime());
    }

    public static LocalDate randomLocalDate(LocalDate after) {
        var afterAsInstant = Instant.from(after.atStartOfDay().toInstant(CURRENT_ZONE_OFFSET));
        return LocalDate.ofInstant(randomInstant(afterAsInstant), ZONE_ID);
    }

    public static Instant randomInstant() {
        return FAKER.date().between(Date.from(BEGINNING_OF_TIME), Date.from(END_OF_TIME)).toInstant();
    }

    public static Instant randomInstant(Instant after) {
        return FAKER.date().between(Date.from(after), Date.from(END_OF_TIME)).toInstant();
    }

    public static String randomJson() {
        var root = objectMapper.createObjectNode();
        for (int i = 0; i < ARBITRARY_FIELDS_NUMBER; i++) {
            root.set(randomString(), randomFlatJson());
        }
        return attempt(() -> objectMapper.writeValueAsString(root)).orElseThrow();
    }

    public static String randomIssn() {
        return IssnGenerator.randomIssn();
    }

    public static String randomInvalidIssn() {
        return IssnGenerator.randomInvalidIssn();
    }

    private static ObjectNode randomFlatJson() {
        var root = objectMapper.createObjectNode();
        for (int i = 0; i < ARBITRARY_FIELDS_NUMBER; i++) {
            root.put(randomString(), randomString());
        }
        return root;
    }
}
