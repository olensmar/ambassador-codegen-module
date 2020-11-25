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

The codegen template has two configuration options that can be provided either by command-line or via OAS extensions in
your spec.

The command-line arguments are

- targetService: specifies the target service to use in the generated mapping file - required
- targetNamespace: specifies the target namespace to use in the generated mapping file, default is "ambassador"
- overrideExtensions: boolean that controls if command-line arguments should override any extensions in the source OAS
  definition, default is false

The corresponding OAS extensions are specified under a "x-ambassador" node at either the top or operation level in your
OAS definition:

```
x-ambassador:
  service: myservice
  namespace: mynamespace
```

## Sample Using with Ambassador

Here comes a quick walkthrough showing howing this all works with Ambassador - using
the [sample petstore service](https://github.com/swagger-api/swagger-petstore).

You'll need to have Ambassador running in a local cluster/namespace as created by the
Ambassador [Getting Started](https://www.getambassador.io/docs/latest/tutorials/getting-started/) guide.

Start by deploying petstore using the provided deployment file

```
kubectl apply -f petstore-deployment.yaml 
```

Make sure petstore has been deployed alongside all the other Ambassador services;

```
> kubectl get pods -n ambassador
NAME                                   READY   STATUS    RESTARTS   AGE
ambassador-97dd6c4cb-v2vsx             1/1     Running   89         54d
ambassador-injector-7d7bdcf58b-phlsx   1/1     Running   1          54d
ambassador-redis-6594476754-wv569      1/1     Running   1          54d
inventory-6cb9bf6465-vg877             1/1     Running   1          53d
petstore-6b6bf5f798-xqxf4              1/1     Running   0          13s
telepresence-proxy-7d4f567fb9-s8slz    1/1     Running   5          54d
```

Now run the provided test profile to generate Ambassador mapping files from the Petstore V3 OAS definition;

```
mvn test -P test
```

The mappings are all generated into the target/ambassador folder - apply the PetApi mappting using

```
kubectl apply -f target/ambassador/PetApi-mapping.yaml 
```

Petstore is now mapped using the Ambassador API Gateway - curl away!

```
> curl http://localhost:8080/api/v3/pet/findByStatus?status=available
[{"id":1,"category":{"id":2,"name":"Cats"},"name":"Cat 1","photoUrls":["url1","url2"],"tags":[{"id":1,"name":"tag1"},{"id":2,"name":"tag2"}],"status":"available"},{"id":2,"category":{"id":2,"name":"Cats"},"name":"Cat 2","photoUrls":["url1","url2"],"tags":[{"id":1,"name":"tag2"},{"id":2,"name":"tag3"}],"status":"available"},{"id":4,"category":{"id":1,"name":"Dogs"},"name":"Dog 1","photoUrls":["url1","url2"],"tags":[{"id":1,"name":"tag1"},{"id":2,"name":"tag2"}],"status":"available"},{"id":7,"category":{"id":4,"name":"Lions"},"name":"Lion 1","photoUrls":["url1","url2"],"tags":[{"id":1,"name":"tag1"},{"id":2,"name":"tag2"}],"status":"available"},{"id":8,"category":{"id":4,"name":"Lions"},"name":"Lion 2","photoUrls":["url1","url2"],"tags":[{"id":1,"name":"tag2"},{"id":2,"name":"tag3"}],"status":"available"},{"id":9,"category":{"id":4,"name":"Lions"},"name":"Lion 3","photoUrls":["url1","url2"],"tags":[{"id":1,"name":"tag3"},{"id":2,"name":"tag4"}],"status":"available"},{"id":10,"category":{"id":3,"name":"Rabbits"},"name":"Rabbit 1","photoUrls":["url1","url2"],"tags":[{"id":1,"name":"tag3"},{"id":2,"name":"tag4"}],"status":"available"}]
```

```
> curl http://localhost:8080/api/v3/pet/2
{"id":2,"category":{"id":2,"name":"Cats"},"name":"Cat 2","photoUrls":["url1","url2"],"tags":[{"id":1,"name":"tag2"},{"id":2,"name":"tag3"}],"status":"available"}
```

If you want to access the UI of the deployed Petstore you'll need to map that with Ambassador also