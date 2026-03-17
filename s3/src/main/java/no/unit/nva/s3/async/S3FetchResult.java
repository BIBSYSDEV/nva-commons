package no.unit.nva.s3.async;

import java.net.URI;
import java.util.Optional;

public final class S3FetchResult<T> {

    private final URI uri;
    private final T content;
    private final Exception error;
    private final boolean success;

    S3FetchResult(URI key, T content, Exception error, boolean success) {
        this.uri = key;
        this.content = content;
        this.error = error;
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public URI getUri() {
        return uri;
    }

    public Optional<T> getContent() {
        return Optional.ofNullable(content);
    }

    public Optional<Exception> getError() {
        return Optional.ofNullable(error);
    }
}
