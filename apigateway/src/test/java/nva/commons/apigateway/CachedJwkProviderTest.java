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

  private static final String KEY_ID = "key-1";

  @Test
  void shouldReturnCachedJwkOnSubsequentCalls() throws JwkException {
    var delegate = mock(JwkProvider.class);
    var jwk = mock(Jwk.class);
    when(delegate.get(KEY_ID)).thenReturn(jwk);

    var provider = new CachedJwkProvider(delegate, 1, TimeUnit.HOURS);

    var first = provider.get(KEY_ID);
    var second = provider.get(KEY_ID);

    assertThat(first).isSameAs(jwk);
    assertThat(second).isSameAs(jwk);
    verify(delegate, times(1)).get(KEY_ID);
  }

  @Test
  void shouldDelegateForDifferentKeyIds() throws JwkException {
    var delegate = mock(JwkProvider.class);
    var jwk1 = mock(Jwk.class);
    var jwk2 = mock(Jwk.class);
    when(delegate.get(KEY_ID)).thenReturn(jwk1);
    when(delegate.get("key-2")).thenReturn(jwk2);

    var provider = new CachedJwkProvider(delegate, 1, TimeUnit.HOURS);

    assertThat(provider.get(KEY_ID)).isSameAs(jwk1);
    assertThat(provider.get("key-2")).isSameAs(jwk2);
  }

  @Test
  void shouldRefetchAfterExpiry() throws JwkException {
    var delegate = mock(JwkProvider.class);
    var jwk = mock(Jwk.class);
    when(delegate.get(KEY_ID)).thenReturn(jwk);

    var provider = new CachedJwkProvider(delegate, 0, TimeUnit.MILLISECONDS);

    provider.get(KEY_ID);
    provider.get(KEY_ID);

    verify(delegate, times(2)).get(KEY_ID);
  }

  @Test
  void shouldPropagateExceptionFromDelegate() throws JwkException {
    var delegate = mock(JwkProvider.class);
    when(delegate.get(KEY_ID)).thenThrow(new JwkException("not found"));

    var provider = new CachedJwkProvider(delegate, 1, TimeUnit.HOURS);

    assertThatThrownBy(() -> provider.get(KEY_ID))
        .isInstanceOf(JwkException.class)
        .hasMessage("not found");
  }
}
