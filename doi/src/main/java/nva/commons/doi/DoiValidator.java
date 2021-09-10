package nva.commons.doi;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoiValidator {

    public static final Pattern DOI_URL_PATTERN =
        Pattern.compile("^https?://(?:dx\\.)?doi\\.org/10\\.[\\w\\d][\\w\\d.]+/.+$",
                        Pattern.CASE_INSENSITIVE);

    public static final Pattern DOI_STRING_PATTERN =
        Pattern.compile("^(doi:)?10\\.[\\w\\d][\\w\\d.]+/.+$", Pattern.CASE_INSENSITIVE);
    public static final String INVALID_DOI_ERROR = "Invalid DOI";
    private static final Logger logger = LoggerFactory.getLogger(DoiValidator.class);
    private final UnitHttpClient httpClient;

    @JacocoGenerated
    public DoiValidator() {
        this(new UnitHttpClient());
    }

    public DoiValidator(UnitHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public static Boolean validateOrThrow(String doi) {
        if (validateOffline(doi)) {
            return true;
        } else {
            logger.error(INVALID_DOI_ERROR + doi);
        }
        throw new InvalidDoiException(doi);
    }

    /**
     * Validates a DOI against URL and String patterns.
     *
     * @param doi the DOI to validate
     * @return true if DOI is valid
     */
    public static boolean validateOffline(String doi) {
        if (isNull(doi)) {
            return false;
        }
        Matcher urlMatcher = DOI_URL_PATTERN.matcher(doi);
        Matcher stringMatcher = DOI_STRING_PATTERN.matcher(doi);
        return urlMatcher.matches() || stringMatcher.matches();
    }

    /**
     * Validates a DOI against URL and String patterns.
     *
     * @param doi the DOI to validate
     * @return true if DOI is valid
     */
    public static boolean validateOffline(URI doi) {
        if (isNull(doi)) {
            return false;
        }
        return validateOffline(doi.toString());
    }

    public Boolean validateOnline(URI doi) {
        HttpResponse<String> httpResponse = resolve(doi);
        if (doiHasBeenResolved(httpResponse)) {
            return true;
        } else {
            throw new UnresolvableDoiException(responseToString(httpResponse));
        }
    }

    private static Boolean doiHasBeenResolved(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        return statusCode == HttpURLConnection.HTTP_MOVED_TEMP
               || statusCode == HttpURLConnection.HTTP_MOVED_PERM
               || statusCode == HttpURLConnection.HTTP_NOT_MODIFIED
               || statusCode == HttpURLConnection.HTTP_OK;
    }

    private static String responseToString(HttpResponse<String> response) {
        return String.format("ResponseStatusCode:%s, ResponseBody:%s",
                             response.statusCode(),
                             response.body());
    }

    private HttpResponse<String> resolve(URI doi) {
        return attempt(() -> resolveAsync(doi).get()).orElseThrow();
    }

    private CompletableFuture<HttpResponse<String>> resolveAsync(URI doi) {
        Builder getRequest = HttpRequest.newBuilder(doi);
        return httpClient.sendAsync(getRequest, BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}