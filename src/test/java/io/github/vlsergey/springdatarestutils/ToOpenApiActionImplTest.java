package io.github.vlsergey.springdatarestutils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.core.validation.ValidationResults;
import org.openapi4j.core.validation.ValidationSeverity;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.validation.v3.OpenApi3Validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ToOpenApiActionImplTest {

    private TaskProperties taskProperties;

    static void assertEquals(URL expectedUrl, File actualFile) throws IOException {
	String expected;
	try (InputStream in = expectedUrl.openStream()) {
	    expected = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r", "");
	}

	String actual = Files.readString(actualFile.toPath(), StandardCharsets.UTF_8).replace("\r", "");
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
	    generate(ToOpenApiActionImplTest.class.getPackageName() + ".test", tempFile);
	    assertOpenAPISpecValid(tempFile.toURI().toURL());
	});
    }

    @Test
    void testExample() throws Exception {
	withTempFile(tempFile -> {
	    generate(ToOpenApiActionImplTest.class.getPackageName() + ".example", tempFile);
	    assertOpenAPISpecValid(tempFile.toURI().toURL());
	    assertEquals(ToOpenApiActionImplTest.class.getResource("example/expected.yaml"), tempFile);
	});
    }

    @Test
    void testProjections() throws Exception {
	withTempFile(tempFile -> {
	    generate(ToOpenApiActionImplTest.class.getPackageName() + ".projections", tempFile);
	    assertOpenAPISpecValid(tempFile.toURI().toURL());
	    assertEquals(ToOpenApiActionImplTest.class.getResource("projections/expected.yaml"), tempFile);
	});
    }

    @FunctionalInterface
    interface FailableConsumer<O> {
	void accept(O object) throws Exception;
    }
}
