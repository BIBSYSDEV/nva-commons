package no.unit.nva.stubs;

import java.net.Socket;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import nva.commons.core.JacocoGenerated;
import org.junit.jupiter.api.Assertions;

@JacocoGenerated
public final class WiremockHttpClient {

    public static final String TEST_CONFIGURATION_TRUST_MANAGER_FAILURE =
        "Failed to configure the trust everything rule for the http client, which is required to connect to "
        + "wiremock server and local signed SSL certificate for now.";

    private WiremockHttpClient() {

    }

    public static HttpClient create() {
        return HttpClient.newBuilder().sslContext(createInsecureSslContextTrustingEverything()).build();
    }

    @SuppressWarnings("PMD.AvoidPrintStackTrace")
    private static SSLContext createInsecureSslContextTrustingEverything() {
        try {
            var insecureSslContext = SSLContext.getInstance("SSL");
            insecureSslContext.init(null, new X509ExtendedTrustManager[]{createTrustEverythingManager()},
                                    new java.security.SecureRandom());
            return insecureSslContext;
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            Assertions.fail(TEST_CONFIGURATION_TRUST_MANAGER_FAILURE);
            return null;
        }
    }

    @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
    private static X509ExtendedTrustManager createTrustEverythingManager() {

        return new X509ExtendedTrustManager() {

            @JacocoGenerated
            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @JacocoGenerated
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            @JacocoGenerated
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @JacocoGenerated
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
            }

            @JacocoGenerated
            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
            }

            @JacocoGenerated
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
            }

            @JacocoGenerated
            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
            }
        };
    }
}

