package nva.commons.apigateway;

public class ProxyResponse<T> {
    
    private final Integer statusCode;
    private final T body;
    
    public ProxyResponse(int statusCode, T body) {
        this.statusCode = statusCode;
        this.body = body;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public T getBody() {
        return body;
    }
}
