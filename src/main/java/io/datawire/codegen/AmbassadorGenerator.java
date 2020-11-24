package io.datawire.codegen;

import io.swagger.codegen.v3.CliOption;
import io.swagger.codegen.v3.CodegenOperation;
import io.swagger.codegen.v3.CodegenType;
import io.swagger.codegen.v3.generators.DefaultCodegenConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmbassadorGenerator extends DefaultCodegenConfig {

    // source folder where to write the files
    protected String sourceFolder = "src";
    protected String apiVersion = "1.0.0";
    private String basePath = "";
    private String targetService;
    private boolean overrideExtensions;

    /**
     * Configures the type of generator.
     *
     * @return the CodegenType for this generator
     * @see io.swagger.codegen.CodegenType
     */
    public CodegenType getTag() {
        return CodegenType.CONFIG;
    }

    /**
     * Configures a friendly name for the generator.  This will be used by the generator
     * to select the library with the -l flag.
     *
     * @return the friendly name for the generator
     */
    public String getName() {
        return "ambassadorGenerator";
    }

    /**
     * Returns human-friendly help for the generator.  Provide the consumer with help
     * tips, parameters here
     *
     * @return A string value for the help message
     */
    public String getHelp() {
        return "Generates an Ambassador configuration file.";
    }

    public AmbassadorGenerator() {
        super();

        // set the output folder here
        outputFolder = "target/generated/ambassador";
        this.cliOptions.add(CliOption.newString("targetService", "The name of the service to map requests to"));
        this.cliOptions.add(CliOption.newBoolean("overrideExtensions", "If a specified targetService should override service extensions"));

        apiTemplateFiles.put(
                "api.mustache",   // the template to use
                "-mapping.yaml");       // the extension for each file to write

        templateDir = "ambassadorCodegen";
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (this.additionalProperties.containsKey("targetService")) {
            targetService = this.additionalProperties.get("targetService").toString();
        }

        if (this.additionalProperties.containsKey("overrideExtensions")) {
            overrideExtensions = Boolean.valueOf(this.additionalProperties.get("overrideExtensions").toString());
        }
    }

    /**
     * Location to write api files.  You can use the apiPackage() as defined when the class is
     * instantiated
     */
    @Override
    public String apiFileFolder() {
        return outputFolder;
    }

    @Override
    public String getArgumentsLocation() {
        return null;
    }

    @Override
    protected String getTemplateDir() {
        return templateDir;
    }

    @Override
    public String getDefaultTemplateDir() {
        return templateDir;
    }

    @Override
    public void preprocessOpenAPI(OpenAPI openAPI) {
        super.preprocessOpenAPI(openAPI);

        initializeBasePath(openAPI);

        if (targetService == null || !overrideExtensions) {
            Map<String, Object> extensions = openAPI.getExtensions();
            if (extensions != null && extensions.containsKey("x-ambassador")) {
                Map<Object, String> values = (Map<Object, String>) extensions.get("x-ambassador");
                targetService = values.get("service");
            }
        }
    }

    private void initializeBasePath(OpenAPI openAPI) {
        List<Server> servers = openAPI.getServers();
        if (servers != null && !servers.isEmpty()) {
            Server server = servers.get(0);
            if (server.getUrl().startsWith("/")) {
                basePath = server.getUrl();
            }
            try {
                URL url = new URL(server.getUrl());
                basePath = url.getPath();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {

        List<CodegenOperation> operations = (List<CodegenOperation>) ((HashMap) objs.get("operations")).get("operation");

        // rewrite path parameters to regex expression
        for (CodegenOperation operation : operations) {
            operation.path = basePath + operation.path.replaceAll("\\{.*}", "\\\\{.*}");

            if (targetService != null) {

                Map<String, Object> extensions = operation.vendorExtensions;
                if (extensions == null) {
                    extensions = new HashMap<>();
                    operation.vendorExtensions = extensions;
                }

                Map<Object, String> values = (Map<Object, String>) extensions.get("x-ambassador");

                if (values == null) {
                    values = new HashMap<>();
                    extensions.put("x-ambassador", values);
                }

                if (overrideExtensions || !values.containsKey("service")) {
                    values.put("service", targetService);
                }
            }
        }

        return super.postProcessOperations(objs);
    }
}