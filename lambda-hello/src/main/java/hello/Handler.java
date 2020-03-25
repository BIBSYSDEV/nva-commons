package hello;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.Collections;
import nva.commons.ApiGatewayHandler;
import nva.commons.RequestInfo;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

public class Handler extends ApiGatewayHandler<String, String> {

    public Handler() {
        super(String.class);
    }

    @Override
    protected String processInput(String input, RequestInfo requestInfo, Context context) throws Exception {

        String envVariable = this.environment.readEnv("MY_ENV");
        String contentLocation = requestInfo.getHeaders().get(HttpHeaders.CONTENT_LOCATION);

        addConentMd5Header(input);

        String output = envVariable + contentLocation + input;
        return output;
    }

    private void addConentMd5Header(String input) {
        byte[] md5 = DigestUtils.md5(input);
        setAdditionalHeadersSupplier(
            () -> Collections.singletonMap(HttpHeaders.CONTENT_MD5, new String(md5))
        );
    }

    @Override
    protected int getFailureStatusCode(String input, Exception error) {

        if (error instanceof IllegalArgumentException) {
            return HttpStatus.SC_BAD_REQUEST;
        } else if (error instanceof IllegalStateException) {
            return HttpStatus.SC_INTERNAL_SERVER_ERROR;
        } else {
            return HttpStatus.SC_INTERNAL_SERVER_ERROR;
        }
    }

    @Override
    protected Integer getSuccessStatusCode(String input, String output) {
        return HttpStatus.SC_OK;
    }
}
