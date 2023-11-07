package io.github.vlsergey.springdatarestutils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.core.validation.ValidationResults;
import org.openapi4j.core.validation.ValidationSeverity;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.validation.v3.OpenApi3Validator;
import org.springframework.security.access.annotation.Secured;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vlsergey.springdatarestutils.example.SingleLine;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.Yaml;

class ToOpenApiActionImplTest {

    private static final String MY_PACKAGE = "io.github.vlsergey.springdatarestutils";

    private TaskProperties taskProperties;

    static void assertEquals(URL expectedUrl, File actualFile) throws IOException {
	Yaml yaml = new Yaml(new Constructor(Map.class));
	Map<String, Object> expected = yaml.load(Resources.toString(expectedUrl, StandardCharsets.UTF_8).replace("\r", ""));
	Map<String, Object> actual = yaml.load(new String(Files.readAllBytes(actualFile.toPath()), StandardCharsets.UTF_8).replace("\r", ""));
	Assertions.assertEquals(expected, actual);
    }

    private static void assertOpenAPISpecValid(final URL specUrl) throws ResolutionException, ValidationException {
	OpenApi3 api = new OpenApi3Parser().parse(specUrl, false);
	ValidationResults results = OpenApi3Validator.instance().validate(api);
	assertTrue(results.isValid());
	assertTrue(results.items(ValidationSeverity.ERROR).isEmpty());
	assertTrue(results.items(ValidationSeverity.WARNING).isEmpty());
    }

    static void withTempFile(FailableConsumer<File> consumer) throws Exception {
	File temp = File.createTempFile(ToOpenApiActionImplTest.class.getSimpleName(), ".yaml");
	try {
	    consumer.accept(temp);
	} finally {
	    if (!temp.delete()) {
		temp.deleteOnExit();
	    }
	}
    }

    @BeforeEach
    void beforeEach() {
	taskProperties = new TaskProperties();
    }

    private void generate(final String basePackage, File dst) throws Exception, JsonProcessingException {
	taskProperties.setBasePackage(basePackage);
	taskProperties.setOutputUri(dst.toURI().toString());

	new ToOpenApiActionImpl(ToOpenApiActionImplTest.class.getSimpleName(), "0.0.1-SNAPSHOT",
		new ObjectMapper().writeValueAsString(taskProperties)).executeWithinUrlClassLoader();
    }

    @Test
    void test() throws Exception {
	withTempFile(tempFile -> {
	    generate(MY_PACKAGE + ".test", tempFile);
	    assertOpenAPISpecValid(tempFile.toURI().toURL());
	});
    }

    @ParameterizedTest
    @CsvSource({ "baserepo", "customfinders", "disablesave", "example", "hibernate", "inheritance", "projections",
	    "withtransient", "userprojectroles", "with_list_of_embeddables", "with_list_of_strings",
	    "with_nonexposed_one_to_many", "with_string_to_string_map", "withembedded", "withenum", "withsecured" })
    void test(String code) throws Exception {
	taskProperties.setAddXCustomAnnotations(Arrays.asList(Secured.class.getName(), SingleLine.class.getName()));

	withTempFile(tempFile -> {
	    generate(MY_PACKAGE + "." + code, tempFile);
	    assertOpenAPISpecValid(tempFile.toURI().toURL());
	    assertEquals(ToOpenApiActionImplTest.class.getResource("expected-" + code + ".yaml"), tempFile);
	});
    }

    @FunctionalInterface
    interface FailableConsumer<O> {
	void accept(O object) throws Exception;
    }
}
