package io.datawire.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.parser.ObjectMapperFactory;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class AmbassadorGeneratorTest {

    @Test
    void validateGeneratedFile() throws IOException {
        ObjectMapper mapper = ObjectMapperFactory.createYaml();
        JsonNode tree = mapper.readTree(new File("target/generated-test-sources/PetApi.sample"));
    }

}