package no.unit.nva.clients;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.HttpStatusCode.BAD_GATEWAY;
import static software.amazon.awssdk.http.HttpStatusCode.NOT_FOUND;
import static software.amazon.awssdk.http.HttpStatusCode.OK;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import no.unit.nva.clients.cristin.CristinClient;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CristinClientTest {

    private HttpClient httpClient;
    private CristinClient cristinClient;
    private TestAppender appender;

    @BeforeEach
    public void setup() {
        httpClient = mock(HttpClient.class);
        cristinClient = new CristinClient(httpClient);
        appender = LogUtils.getTestingAppender(CristinClient.class);
    }

    @Test
    void shouldReturnOptionalEmptyWhenCouldNotFetchPersonWhenPersonDoesNotExistsAndLogNotFoundMessage()
        throws IOException, InterruptedException {
        var cristinPersonId = randomUri();
        var request = HttpRequest.newBuilder().GET().uri(cristinPersonId).build();

        when(httpClient.send(eq(request), any())).thenReturn(FakeHttpResponse.create(request, null, NOT_FOUND));

        var person = cristinClient.getPerson(cristinPersonId);
        var loggMessage = appender.getMessages();

        assertTrue(person.isEmpty());
        assertTrue(loggMessage.contains("Cristin responded with not found: " + cristinPersonId));
    }

    @Test
    void shouldReturnOptionalEmptyWhenUnknownProblemsAndLogUnknownProblemMessage()
        throws IOException, InterruptedException {
        var cristinPersonId = randomUri();
        var request = HttpRequest.newBuilder().GET().uri(cristinPersonId).build();

        when(httpClient.send(eq(request), any())).thenReturn(FakeHttpResponse.create(request, null, BAD_GATEWAY));

        var person = cristinClient.getPerson(cristinPersonId);
        var loggMessage = appender.getMessages();

        assertTrue(person.isEmpty());
        assertTrue(loggMessage.contains("Cristin responded with 502 when fetching: " + cristinPersonId));
    }

    @Test
    void shouldReturnOptionalPresentWhenFetchingCristinPerson() throws IOException, InterruptedException {
        var cristinPersonId = randomUri();
        var request = HttpRequest.newBuilder().GET().uri(cristinPersonId).build();

        when(httpClient.send(eq(request), any())).thenReturn(
            FakeHttpResponse.create(request, cristinPersonResponseBody().replace("_ID_", cristinPersonId.toString()),
                                    OK));

        var person = cristinClient.getPerson(cristinPersonId).orElseThrow();

        assertEquals("X Y", person.fullName());
        assertEquals(cristinPersonId, person.id());
    }

    @Test
    void shouldReturnOptionalEmptyWhenCouldNotFetchOrganizationWhenOrganizationDoesNotExistsAndLogNotFoundMessage()
        throws IOException, InterruptedException {
        var organizationId = randomUri();
        var request = HttpRequest.newBuilder().GET().uri(organizationId).build();

        when(httpClient.send(eq(request), any())).thenReturn(FakeHttpResponse.create(request, null, NOT_FOUND));

        var organization = cristinClient.getOrganization(organizationId);
        var loggMessage = appender.getMessages();

        assertTrue(organization.isEmpty());
        assertTrue(loggMessage.contains("Cristin responded with not found: " + organizationId));
    }

    @Test
    void shouldReturnOptionalPresentWhenFetchingCristinOrganization() throws IOException, InterruptedException {
        var organizationId = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/ABCD.X.0.0");
        var request = HttpRequest.newBuilder().GET().uri(organizationId).build();

        when(httpClient.send(eq(request), any())).thenReturn(FakeHttpResponse.create(request,
                                                                                     cristinOrganizationResponseBody().replace(
                                                                                         "_ID_",
                                                                                         organizationId.toString()),
                                                                                     OK));

        var organization = cristinClient.getOrganization(organizationId).orElseThrow();

        var expectedTopLevelOrg = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/ABCD.0.0.0");
        var expectedSubOrg = URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/ABCD.X.Y.0");

        assertEquals(organizationId, organization.id());
        assertEquals(expectedTopLevelOrg, organization.getTopLevelOrganization().id());
        assertEquals(expectedSubOrg, organization.hasPart().get(0).id());
    }

    @Test
    void shouldReturnPersonByIdentifier() throws IOException, InterruptedException {
        var personIdentifier = randomString();
        var requestUri = uriWithPathParams("cristin", "person", personIdentifier);
        var request = HttpRequest.newBuilder().GET().uri(requestUri).build();

        when(httpClient.send(eq(request), any())).thenReturn(
            FakeHttpResponse.create(request, cristinPersonResponseBody().replace("_ID_", personIdentifier),
                                    OK));

        var organization = cristinClient.getPerson(personIdentifier);

        assertTrue(organization.isPresent());
    }

    @Test
    void shouldReturnOrganizationByIdentifier() throws IOException, InterruptedException {
        var organizationIdentifier = randomString();
        var requestUri = uriWithPathParams("cristin", "organization", organizationIdentifier);
        var request = HttpRequest.newBuilder().GET().uri(requestUri).build();

        when(httpClient.send(eq(request), any())).thenReturn(
            FakeHttpResponse.create(request, cristinOrganizationResponseBody().replace("_ID_", organizationIdentifier),
                                    OK));

        var organization = cristinClient.getOrganization(organizationIdentifier);

        assertTrue(organization.isPresent());
    }

    private static URI uriWithPathParams(String... params) {
        return UriWrapper.fromHost(new Environment().readEnv("API_HOST")).addChild().addChild(params).getUri();
    }

    private String cristinOrganizationResponseBody() {
        return """
                        {
              "@context": "https://bibsysdev.github.io/src/organization-context.json",
              "type": "Organization",
              "id": "https://api.dev.nva.aws.unit.no/cristin/organization/ABCD.X.0.0",
              "labels": {
                "nb": "Enhet X"
              },
              "acronym": "X",
              "country": "NO",
              "partOf": [
                {
                  "type": "Organization",
                  "id": "https://api.dev.nva.aws.unit.no/cristin/organization/ABCD.0.0.0",
                  "labels": {
                    "en": "University X",
                    "nb": "Universitet X"
                  },
                  "acronym": "X",
                  "country": "NO",
                  "partOf": [],
                  "hasPart": []
                }
              ],
              "hasPart": [
                {
                  "type": "Organization",
                  "id": "https://api.dev.nva.aws.unit.no/cristin/organization/ABCD.X.Y.0",
                  "labels": {
                    "nb": "Fagdirektør"
                  },
                  "acronym": "X",
                  "partOf": [
                    {
                      "type": "Organization",
                      "id": "https://api.dev.nva.aws.unit.no/cristin/organization/ABCD.X.0.0",
                      "labels": {},
                      "partOf": [],
                      "hasPart": []
                    }
                  ],
                  "hasPart": []
                }
              ]
            }
            
            """;
    }

    private String cristinPersonResponseBody() {

        return """
                       {
              "@context": "https://example.org/person-context.json",
              "id": "_ID_",
              "type": "Person",
              "identifiers": [
                {
                  "type": "CristinIdentifier",
                  "value": "ABCDEF"
                }
              ],
              "names": [
                {
                  "type": "FirstName",
                  "value": "X"
                },
                {
                  "type": "LastName",
                  "value": "Y"
                }
              ],
              "affiliations": [
                {
                  "type": "Affiliation",
                  "organization": "https://api.dev.nva.aws.unit.no/cristin/organization/ABCD.X.Y.Z",
                  "active": true,
                  "role": {
                    "type": "Role",
                    "labels": {
                      "en": "Head physician",
                      "nb": "Overlege"
                    }
                  }
                }
              ],
              "verified": true
            }
            """;
    }
}
