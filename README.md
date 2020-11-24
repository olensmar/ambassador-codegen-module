# Ambassador Swagger Codegen Module

This project provides a [Swagger Codegen 3.X](https://github.com/swagger-api/swagger-codegen/tree/3.0.0) template for 
generating [Ambassador Mapping resources](https://www.getambassador.io/docs/latest/topics/using/intro-mappings/) 
from OAS/Swagger definitions.

## Usage

Clone this repo and build with 

```
mvn clean install
```

and use with

```
java -cp <path to swagger-codegen-cli>:target/ambassador-swagger-codegen-1.0.0.jar \
   io.swagger.codegen.v3.cli.SwaggerCodegen generate \
   -l ambassadorGenerator \
   -i <path/url to OAS definition> \
   -o <output folder>
```

## Configuration