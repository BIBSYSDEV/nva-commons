# NVA Commons

Java utilities to providing unified implementation across the NVA project.

## Subprojects

### apigateway

Unifies the approach to handling [AWS API-Gateway](https://aws.amazon.com/api-gateway/) events
in [AWS Lambdas](https://aws.amazon.com/lambda/)
using [AWS Java SDK version 1](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/welcome.html).

See Choosing SDK v1 or v2.

### apigatewayv2

Unifies the approach to handling [AWS API-Gateway](https://aws.amazon.com/api-gateway/) events
in [AWS Lambdas](https://aws.amazon.com/lambda/) using
the [AWS Java SDK version 2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html).

See choosing SDK v1 or v2

```groovy
implementation group: 'com.github.bibsysdev', name: 'apigatewayv2', version: '$version'
```

### core

### doi

### dynamodb

### eventhandlers

### identifiers

### json

### lambdaauthorizer

### logutils

### nvatestutils

### s3

### secrets