package nva.commons.apigateway;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import static java.util.Objects.nonNull;

public class CachedJwkProvider implements JwkProvider {

    private final JwkProvider delegate;
    private final long ttlMillis;
    private final ConcurrentHashMap<String, CachedEntry> cache = new ConcurrentHashMap<>();

    public CachedJwkProvider(JwkProvider delegate, long expiresIn, TimeUnit unit) {
        this.delegate = delegate;
        this.ttlMillis = unit.toMillis(expiresIn);
    }

    @Override
    public Jwk get(String keyId) throws JwkException {
        var entry = cache.get(keyId);
        if (nonNull(entry) && entry.isValid()) {
            return entry.jwk();
        }
        var jwk = delegate.get(keyId);
        cache.put(keyId, new CachedEntry(jwk, Instant.now().plusMillis(ttlMillis)));
        return jwk;
    }

    private record CachedEntry(Jwk jwk, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }
}
