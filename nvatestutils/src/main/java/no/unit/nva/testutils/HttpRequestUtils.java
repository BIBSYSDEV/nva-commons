package no.unit.nva.testutils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;
import org.mockito.invocation.InvocationOnMock;

@JacocoGenerated
public class HttpRequestUtils {

    public CompletableFuture<HttpResponse<String>> echoRequestAsFuture(InvocationOnMock invocation) {
        return CompletableFuture.completedFuture(echoRequest(invocation));
    }

    public HttpResponse<String> echoRequest(InvocationOnMock invocation) {
        HttpRequest request = invocation.getArgument(0);
        String body = RequestBodyReader.requestBody(request);
        HttpHeaders headers = mockHeaders(request);
        return mockResponse(body, headers);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockResponse(String body, HttpHeaders headers) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(HttpStatus.SC_OK);
        when(response.body()).thenReturn(body);
        when(response.headers()).thenReturn(headers);
        return response;
    }

    protected HttpHeaders mockHeaders(HttpRequest request) {
        return request.headers();
    }
}
