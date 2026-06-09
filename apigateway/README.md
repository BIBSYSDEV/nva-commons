# ApiGateway

This library provides Java utilities that provide a uniform way of working with AWS API gateway events in AWS Lambdas.

#### Install

Since most projects extend the ApiGatewayHandler class and use the ApiGatewayException, use the _implementation_
configuration.

```groovy
implementation group: 'com.github.bibsysdev', name: 'apigateway', version: '$version'
```

### AccessRight

This enum provides the known access rights in the NVA platform.

### AccessRightEntry

Should this be an exposed API at all? It is used by RequestInfo, HandlerRequestBuilder, but is never accessed directly.

### ApiGatewayHandler

When receiving events from API Gateway, there are three basic concerns:

1. receiving and processing input
2. doing something with the input
3. returning a response to API Gateway

ApiGatewayHandler provides 1 and 3.

#### Basic usage

```java
package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;

import java.net.HttpURLConnection;

public class MyLambdaHandler extends ApiGatewayHandler<InputObject, OutputObject> {

    // You need to set the input class explicitly to ensure that the input is deserialized correctly
    public MyLambdaHandler() {
        super(Input.class);
    }

    // The input is processed and interpreted by the ApiGatewayHandler
    // Whatever you are doing in your business logic returns an object of type OutputObject
    // Errors that occur should translate to an ApiGatewayException
    @Override
    protected OutputObject processInput(InputObject input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        return MyBusinessLogic.doSomethingAndReturnOutputObject(input);
    }

    // The HTTP status code returned by AWS API Gateway when the request succeeds, typically 2XX.
    @Override
    protected Integer getSuccessStatusCode(InputObject input, OutputObject output) {
        return HttpURLConnection.HTTP_OK;
    }
}
```

### ApiMessageParser

This should not be a visible API since it is only used internally.

In the current code, we should do something to prevent users observing this class directly.
Options:

- Package private
- Using module-info.java
- Others?

### GatewayResponse

A testing utility for data that is to be _sent to_ API.

*Note:* Here, there is a potential design issue, as this belongs in a separate testing module ApiGatewayTestUtils, but
is also used internally by the ApiGatewayHandler class.

Usage:

```java
import nva.commons.apigateway.GatewayResponse;

import java.io.ByteArrayOutputStream;

class MyTest {
    private ByteArrayOutputStream output;

    @Test
    void shouldTestSomething() {
        output = myHandlerCode();
        var response = GatewayResponse.fromOutputStream(output, MyOutputBodyClass.class);
        var locationHeader = response.getHeaders().get(HttpHeaders.LOCATION);
        var body = reponse.getBodyObject();
        var statusCode = response.getStatusCode();
        // whatever assertion you had intended inserted below
    }

}
```

### MediaTypes

Adds com.google.common.net.MediaType compatible constants for the media types:

- application/ld+json
- application/problem+json
- application/vnd.datacite.datacite+xml

#### Usage

```java
import static nva.commons.apigateway.MediaTypes.APPLICATION_DATACITE_XML;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.apigateway.MediaTypes.APPLICATION_PROBLEM_JSON;

class MyClass {

    void myMethod() {
        var dataciteMediaType = MediaTypes.APPLICATION_DATACITE_XML;
        var jsonLdMediaType = PageAttributes.MediaTypes.APPLICATION_JSON_LD;
        var problemMediaType = PageAttributes.MediaTypes.APPLICATION_PROBLEM_JSON;
        // use these somehow
    }
}
```

### RequestInfo

Container class for information about an HTTP request from AWS ApiGateway, including:

- String getHeader(String header)
    - returns the specified header
- String getAuthHeader()
    - returns the Authorization header
- String getQueryParameter(String parameter)
    - returns the specified query parameter
- Optional<String> getQueryParameterOpt(String parameter)
    - returns an optional of a potentially missing query parameter
- String getPathParameter(String parameter)
    - returns the specified path parameter
- String getRequestContextParameter(JsonPointer jsonPointer)
    - returns a request context parameter by JSON pointer
    - (*note:* is this used outside the class itself?)
- Optional<String> getRequestContextParameterOpt(JsonPointer jsonPointer)
    - returns an optional of a potentially missing request context parameter by JSON pointer
    - (*note:* is this used outside the class itself?)
- String getMethodArn()
    - returns the ARN of the invoked method
- Map<String, Object> getOtherProperties()
    - returns a list of properties not covered by direct accessors in this class
- Map<String, String> getHeaders()
    - returns a map of all headers in the request
- String getPath()
    - returns the relative path of API-Gateway invocation
- Map<String, String> getPathParameters()
    - returns a map of all path parameters
- Map<String, String> getQueryParameters()
    - returns a map of all query parameters
- JsonNode getRequestContext()
    - returns a JsonNode containing the request Context object
- URI getRequestUri()
    - returns the originating URI for the request
- String getDomainName()
    - returns the domain name used in the request
- URI getCustomerId()
    - returns the Customer URI for the _authorized request_
    - *note:* how does this differ from *getCurrentCustomer()* below?
- String getNvaUsername()
    - returns the requester's username for the _authorized request_
- Optional<URI> getTopLevelOrgCristinId()
    - returns the URI representing the top-level Cristin organization associated with the requesting user for an _
      authorized request_
- URI getCurrentCustomer()
    - returns the URI of the requester's associated customer for an _authorized request_
- URI getPersonCristinId()
    - returns the requester's person ID in the Cristin Person service
- String getPersonNin()
    - returns the requester's national identity number (NIN) as supplied by the Cristin Person service

The class also provides setters for the information, however, these will typically be used in API-testing.

### RequestInfoConstants

This class should likely be package private

### RestConfig

This class should likely be package private

### RestRequestHandler

This is the template class for implementation of handlers that receive AWS API GateWay events.

It should probably be package private.
