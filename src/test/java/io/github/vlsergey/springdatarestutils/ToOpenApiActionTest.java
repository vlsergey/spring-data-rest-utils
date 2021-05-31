package io.github.vlsergey.springdatarestutils;

import java.io.File;
import java.net.URL;
import java.util.function.Function;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.provider.DefaultProperty;
import org.gradle.api.internal.provider.PropertyHost;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.core.validation.ValidationResults;
import org.openapi4j.core.validation.ValidationSeverity;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.validation.v3.OpenApi3Validator;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@ExtendWith(MockitoExtension.class)
class ToOpenApiActionTest {

    @Mock
    RegularFile outputFileMock;

    @Mock
    RegularFileProperty outputPropertyMock;

    @Mock
    PluginProperties pluginProperties;

    @Mock
    Project project;

    @Mock
    PropertyHost propertyHost;

    @Mock
    Task task;

    private static void assertOpenAPISpecValid(final URL specUrl) throws ResolutionException, ValidationException {
	OpenApi3 api = new OpenApi3Parser().parse(specUrl, false);
	ValidationResults results = OpenApi3Validator.instance().validate(api);
	assertTrue(results.isValid());
	assertTrue(results.items(ValidationSeverity.ERROR).isEmpty());
	assertTrue(results.items(ValidationSeverity.WARNING).isEmpty());
    }

    @BeforeEach
    void beforeEach() {
	setProperty(PluginProperties::getAddXLinkedEntity, Boolean.FALSE);
	setProperty(PluginProperties::getAddXSortable, Boolean.FALSE);
	setProperty(PluginProperties::getBasePackage, ToOpenApiActionTest.class.getPackageName() + ".test");
	setProperty(PluginProperties::getInfo, new Info());
	setProperty(PluginProperties::getLinkTypeName, "LinkType");
	setProperty(PluginProperties::getRepositoryDetectionStrategyEnum, RepositoryDetectionStrategies.ALL);
	setProperty(PluginProperties::getTypeSuffix, "");
	setProperty(PluginProperties::getServers, singletonList(new Server().url("/api")));
	setProperty(PluginProperties::getWithLinksTypeSuffix, "WithLinks");

	Mockito.when(project.getDisplayName()).thenReturn(ToOpenApiActionTest.class.getSimpleName());
	Mockito.when(project.getVersion()).thenReturn("0.0.1-SNAPSHOT");
    }

    private void mockOutputProperty(File file) {
	Mockito.when(outputFileMock.getAsFile()).thenReturn(file);
	Mockito.when(outputPropertyMock.get()).thenReturn(outputFileMock);
	Mockito.when(outputPropertyMock.getAsFile()).thenReturn(Mockito.mock(Provider.class, new Returns(file)));
	Mockito.when(pluginProperties.getOutput()).thenReturn(outputPropertyMock);
    }

    @SuppressWarnings("unchecked")
    private <T> void setProperty(Function<PluginProperties, Provider<T>> call, T value) {
	final Property<T> provider = new DefaultProperty<>(propertyHost, (Class<T>) value.getClass()).value(value);
	Mockito.when(call.apply(pluginProperties)).thenReturn(provider);
    }

    @Test
    void test() throws Exception {
	File temp = File.createTempFile(ToOpenApiActionTest.class.getSimpleName(), ".yaml");
	try {
	    mockOutputProperty(temp);

	    ToOpenApiAction action = new ToOpenApiAction(project, pluginProperties);
	    action.executeWithClassLoader(ToOpenApiActionTest.class.getClassLoader());

	    assertOpenAPISpecValid(temp.toURI().toURL());
	} finally {
	    if (!temp.delete()) {
		temp.deleteOnExit();
	    }
	}
    }
}
