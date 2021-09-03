package io.github.vlsergey.springdatarestutils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

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
import org.springframework.security.access.annotation.Secured;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vlsergey.springdatarestutils.example.SingleLine;

class ToOpenApiActionImplTest {

    private static final String MY_PACKAGE = "io.github.vlsergey.springdatarestutils";

    private TaskProperties taskProperties;

    static void assertEquals(URL expectedUrl, File actualFile) throws IOException {
	String expected = Resources.toString(expectedUrl, StandardCharsets.UTF_8).replace("\r", "");
	String actual = new String(Files.readAllBytes(actualFile.toPath()), StandardCharsets.UTF_8).replace("\r", "");
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

    @Test
    void testBaseRepo() throws Exception {
	withTempFile(tempFile -> {
	    generate(MY_PACKAGE + ".baserepo", tempFile);
	    assertOpenAPISpecValid(tempFile.toURI().toURL());
	    assertEquals(ToOpenApiActionImplTest.class.getResource("expected-baserepo.yaml"), tempFile);
	});
    }

    @Test
    void testCustomFinders() throws Exception {
	withTempFile(tempFile -> {
	    generate(MY_PACKAGE + ".customfinders", tempFile);
	    assertOpenAPISpecValid(tempFile.toURI().toURL());
	    assertEquals(ToOpenApiActionImplTest.class.getResource("expected-customfinders.yaml"), tempFile);
	});
    }

    @Test
    void testExample() throws Exception {
	taskProperties.setAddXCustomAnnotations(singletonList(SingleLine.class.getName()));

	withTempFile(tempFile -> {
	    generate(MY_PACKAGE + ".example", tempFile);
	    assertOpenAPISpecValid(tempFile.toURI().toURL());
	    assertEquals(ToOpenApiActionImplTest.class.getResource("expected-example.yaml"), tempFile);
	});
    }

    @Test
    void testHibernate() throws Exception {
	withTempFile(tempFile -> {
	    generate(MY_PACKAGE + ".hibernate", tempFile);
	    assertOpenAPISpecValid(tempFile.toURI().toURL());
	    assertEquals(ToOpenApiActionImplTest.class.getResource("expected-hibernate.yaml"), tempFile);
	});
    }

    @Test
    void testInheritance() throws Exception {
	withTempFile(tempFile -> {
	    generate(MY_PACKAGE + ".inheritance", tempFile);
	    assertOpenAPISpecValid(tempFile.toURI().toURL());
	    assertEquals(ToOpenApiActionImplTest.class.getResource("expected-inheritance.yaml"), tempFile);
	});
    }

    @Test
    void testProjections() throws Exception {
	withTempFile(tempFile -> {
	    generate(MY_PACKAGE + ".projections", tempFile);
	    assertOpenAPISpecValid(tempFile.toURI().toURL());
	    assertEquals(ToOpenApiActionImplTest.class.getResource("expected-projections.yaml"), tempFile);
	});
    }

    @Test
    void testUserProjectRoles() throws Exception {
	taskProperties.setAddXCustomAnnotations(singletonList(SingleLine.class.getName()));

	withTempFile(tempFile -> {
	    generate(MY_PACKAGE + ".userprojectroles", tempFile);
	    assertOpenAPISpecValid(tempFile.toURI().toURL());
	    assertEquals(ToOpenApiActionImplTest.class.getResource("expected-userprojectroles.yaml"), tempFile);
	});
    }

    @Test
    void testWithEnum() throws Exception {
	withTempFile(tempFile -> {
	    generate(MY_PACKAGE + ".withenum", tempFile);
	    assertOpenAPISpecValid(tempFile.toURI().toURL());
	    assertEquals(ToOpenApiActionImplTest.class.getResource("expected-withenum.yaml"), tempFile);
	});
    }

    @Test
    void testWithSecured() throws Exception {
	taskProperties.setAddXCustomAnnotations(Arrays.asList(Secured.class.getName()));

	withTempFile(tempFile -> {
	    generate(MY_PACKAGE + ".withsecured", tempFile);
	    assertOpenAPISpecValid(tempFile.toURI().toURL());
	    assertEquals(ToOpenApiActionImplTest.class.getResource("expected-withsecured.yaml"), tempFile);
	});
    }

    @FunctionalInterface
    interface FailableConsumer<O> {
	void accept(O object) throws Exception;
    }
}
