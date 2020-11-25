package io.datawire.codegen;

import io.swagger.codegen.v3.CliOption;
import io.swagger.codegen.v3.CodegenOperation;
import io.swagger.codegen.v3.CodegenType;
import io.swagger.codegen.v3.generators.DefaultCodegenConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmbassadorGenerator extends DefaultCodegenConfig {
    private static Logger LOGGER = LoggerFactory.getLogger(AmbassadorGenerator.class);

    private String basePath = "";
    private String targetService;
    private boolean overrideExtensions;
    private String targetNamespace = "ambassador";

    public CodegenType getTag() {
        return CodegenType.CONFIG;
    }

    public String getName() {
        return "ambassadorGenerator";
    }

    public String getHelp() {
        return "Generates an Ambassador mapping file.";
    }

    public AmbassadorGenerator() {
        super();

        this.cliOptions.add(CliOption.newString("targetService", "The name of the target service"));
        this.cliOptions.add(CliOption.newString("targetNamespace", "The namespace of the target service"));
        this.cliOptions.add(CliOption.newBoolean("overrideExtensions", "If a specified targetService should override service extensions"));

        apiTemplateFiles.put("api.mustache", "-mapping.yaml");
        templateDir = "ambassadorCodegen";
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (this.additionalProperties.containsKey("targetService")) {
            targetService = this.additionalProperties.get("targetService").toString();
        } else {
            LOGGER.warn("Missing targetService argument - make sure the corresponding x-ambassador.service vendor extension is in your OAS definition");
        }

        if (this.additionalProperties.containsKey("targetNamespace")) {
            targetNamespace = this.additionalProperties.get("targetNamespace").toString();
        } else {
            LOGGER.warn("Missing targetNamespace argument - make sure the corresponding x-ambassador.namespace vendor extension is in your OAS definition");
        }

        if (this.additionalProperties.containsKey("overrideExtensions")) {
            overrideExtensions = Boolean.valueOf(this.additionalProperties.get("overrideExtensions").toString());
        }
    }

    @Override
    public String apiFileFolder() {
        return outputFolder;
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
                if (values.containsKey("service")) {
                    targetService = values.get("service");
                }
                if (values.containsKey("namespace")) {
                    targetNamespace = values.get("namespace");
                }
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

        for (CodegenOperation operation : operations) {
            // rewrite path parameters to regex expression - this probably needs to be improved
            operation.path = basePath + operation.path.replaceAll("\\{.*}", "\\{.*}");

            // make operationId lowercase - required by ambassador where it is used as name in metadata
            operation.operationId = operation.operationId.toLowerCase();

            // add vendor extensions used in template
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
                if (overrideExtensions || !values.containsKey("namespace")) {
                    values.put("namespace", targetNamespace);
                }
            }
        }

        return super.postProcessOperations(objs);
    }
}