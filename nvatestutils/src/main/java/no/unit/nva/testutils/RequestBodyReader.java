package no.unit.nva.testutils;

import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class RequestBodyReader {

    public static final String NO_BODY_PUBLISHER_ERROR = "HttpRequest has no bodyPublisher";

    /**
     * Extracts the body of a {@link HttpRequest} into a String.
     *
     * @param request the {@link HttpRequest}.
     * @return a String containing the contents of the body.
     */
    @JacocoGenerated
    public static String requestBody(HttpRequest request) {
        BodyPublisher bodyPublisher = request.bodyPublisher()
                                          .orElseThrow(() -> new IllegalStateException(NO_BODY_PUBLISHER_ERROR));
        RequestBodySubscriber subscriber = new RequestBodySubscriber();
        bodyPublisher.subscribe(subscriber);
        return subscriber.getBody();
    }

    @JacocoGenerated
    private static final class RequestBodySubscriber implements Subscriber<ByteBuffer> {

        public static final int NUMBER_OF_REQUESTS_TO_PUBLISHER = 1000;

        private final ByteBuffer cache = ByteBuffer.allocate(100);

        public String getBody() {
            return new String(cache.array(), StandardCharsets.UTF_8);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            subscription.request(NUMBER_OF_REQUESTS_TO_PUBLISHER);
        }

        @Override
        public void onNext(ByteBuffer item) {
            cache.put(item);
        }

        @Override
        public void onError(Throwable throwable) {
            System.err.println(throwable.getMessage());
        }

        @Override
        public void onComplete() {

        }
    }
}


