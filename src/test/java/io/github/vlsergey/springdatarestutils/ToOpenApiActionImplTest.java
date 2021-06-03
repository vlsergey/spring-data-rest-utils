package io.github.vlsergey.springdatarestutils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.core.validation.ValidationResults;
import org.openapi4j.core.validation.ValidationSeverity;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.validation.v3.OpenApi3Validator;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;

import com.fasterxml.jackson.databind.ObjectMapper;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

class ToOpenApiActionImplTest {

    private TaskProperties taskProperties;

    private static void assertOpenAPISpecValid(final URL specUrl) throws ResolutionException, ValidationException {
	OpenApi3 api = new OpenApi3Parser().parse(specUrl, false);
	ValidationResults results = OpenApi3Validator.instance().validate(api);
	assertTrue(results.isValid());
	assertTrue(results.items(ValidationSeverity.ERROR).isEmpty());
	assertTrue(results.items(ValidationSeverity.WARNING).isEmpty());
    }

    @BeforeEach
    void beforeEach() {
	taskProperties = new TaskProperties();
	taskProperties.setAddXLinkedEntity(false);
	taskProperties.setAddXSortable(false);
	taskProperties.setInfo(new Info());
	taskProperties.setLinkDepth(3);
	taskProperties.setLinkTypeName("LinkType");
	taskProperties.setRepositoryDetectionStrategy(RepositoryDetectionStrategies.DEFAULT.name());
	taskProperties.setTypeSuffix("");
	taskProperties.setServers(singletonList(new Server().url("/api")));
	taskProperties.setWithLinksTypeSuffix("WithLinks");
    }

    @Test
    void test() throws Exception {
	File temp = File.createTempFile(ToOpenApiActionImplTest.class.getSimpleName(), ".yaml");
	try {
	    taskProperties.setBasePackage(ToOpenApiActionImplTest.class.getPackageName() + ".test");
	    taskProperties.setOutputUri(temp.toURI().toString());

	    new ToOpenApiActionImpl(ToOpenApiActionImplTest.class.getSimpleName(), "0.0.1-SNAPSHOT",
		    new ObjectMapper().writeValueAsString(taskProperties)).executeWithinUrlClassLoader();

	    assertOpenAPISpecValid(temp.toURI().toURL());
	} finally {
	    if (!temp.delete()) {
		temp.deleteOnExit();
	    }
	}
    }

    @Test
    void testExample() throws Exception {
	File temp = File.createTempFile(ToOpenApiActionImplTest.class.getSimpleName(), ".yaml");
	try {
	    taskProperties.setBasePackage(ToOpenApiActionImplTest.class.getPackageName() + ".example");
	    taskProperties.setOutputUri(temp.toURI().toString());

	    new ToOpenApiActionImpl(ToOpenApiActionImplTest.class.getSimpleName(), "0.0.1-SNAPSHOT",
		    new ObjectMapper().writeValueAsString(taskProperties)).executeWithinUrlClassLoader();

	    assertOpenAPISpecValid(temp.toURI().toURL());

	    String expected;
	    try (InputStream in = ToOpenApiActionImplTest.class.getResourceAsStream("example/expected.yaml")) {
		expected = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r", "");
	    }

	    String actual = Files.readString(temp.toPath(), StandardCharsets.UTF_8).replace("\r", "");
	    assertEquals(expected, actual);
	} finally {
	    if (!temp.delete()) {
		temp.deleteOnExit();
	    }
	}
    }
}
