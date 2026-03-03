package nva.commons.apigateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class CachedJwkProviderTest {

    @Test
    void shouldReturnCachedJwkOnSubsequentCalls() throws JwkException {
        var delegate = mock(JwkProvider.class);
        var jwk = mock(Jwk.class);
        when(delegate.get("key-1")).thenReturn(jwk);

        var provider = new CachedJwkProvider(delegate, 1, TimeUnit.HOURS);

        var first = provider.get("key-1");
        var second = provider.get("key-1");

        assertThat(first).isSameAs(jwk);
        assertThat(second).isSameAs(jwk);
        verify(delegate, times(1)).get("key-1");
    }

    @Test
    void shouldDelegateForDifferentKeyIds() throws JwkException {
        var delegate = mock(JwkProvider.class);
        var jwk1 = mock(Jwk.class);
        var jwk2 = mock(Jwk.class);
        when(delegate.get("key-1")).thenReturn(jwk1);
        when(delegate.get("key-2")).thenReturn(jwk2);

        var provider = new CachedJwkProvider(delegate, 1, TimeUnit.HOURS);

        assertThat(provider.get("key-1")).isSameAs(jwk1);
        assertThat(provider.get("key-2")).isSameAs(jwk2);
    }

    @Test
    void shouldRefetchAfterExpiry() throws JwkException {
        var delegate = mock(JwkProvider.class);
        var jwk = mock(Jwk.class);
        when(delegate.get("key-1")).thenReturn(jwk);

        var provider = new CachedJwkProvider(delegate, 0, TimeUnit.MILLISECONDS);

        provider.get("key-1");
        provider.get("key-1");

        verify(delegate, times(2)).get("key-1");
    }

    @Test
    void shouldPropagateExceptionFromDelegate() throws JwkException {
        var delegate = mock(JwkProvider.class);
        when(delegate.get("key-1")).thenThrow(new JwkException("not found"));

        var provider = new CachedJwkProvider(delegate, 1, TimeUnit.HOURS);

        assertThatThrownBy(() -> provider.get("key-1"))
            .isInstanceOf(JwkException.class)
            .hasMessage("not found");
    }
}
