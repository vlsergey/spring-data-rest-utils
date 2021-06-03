package io.github.vlsergey.springdatarestutils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.atteo.evo.inflector.English;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ToOpenApiActionImpl {

    private final String projectDisplayName;
    private final String projectVersion;
    private final TaskProperties taskProperties;

    @SneakyThrows
    public ToOpenApiActionImpl(String projectDisplayName, String projectVersion, String taskProperties) {
	this.projectDisplayName = projectDisplayName;
	this.projectVersion = projectVersion;
	this.taskProperties = new ObjectMapper().readValue(taskProperties, TaskProperties.class);
    }

    public void executeWithinUrlClassLoader() throws Exception {
	final RepositoryEnumerator repositoryEnumerator = new RepositoryEnumerator(this.taskProperties.getBasePackage(),
		RepositoryDetectionStrategies.valueOf(this.taskProperties.getRepositoryDetectionStrategy()));

	final Set<RepositoryMetadata> interfaces = repositoryEnumerator
		.enumerate(Thread.currentThread().getContextClassLoader());
	final Predicate<Class<?>> isExposed = cls -> interfaces.stream()
		.anyMatch(meta -> meta.getDomainType().isAssignableFrom(cls));

	final EntityToSchemaMapper mapper = new EntityToSchemaMapper(isExposed);

	Queue<Pair<Class<?>, ClassMappingMode>> toProcess = new LinkedList<>();
	Set<Pair<Class<?>, ClassMappingMode>> queued = new HashSet<>();

	OpenAPI apiModel = new OpenAPI();
	setApiInfo(apiModel);
	apiModel.setServers(this.taskProperties.getServers());

	interfaces.forEach(meta -> {
	    Pair<Class<?>, ClassMappingMode> key = Pair.<Class<?>, ClassMappingMode>of(meta.getDomainType(),
		    ClassMappingMode.EXPOSED_WITH_LINKS);
	    toProcess.add(key);
	    queued.add(key);
	});

	while (!toProcess.isEmpty()) {
	    final Pair<Class<?>, ClassMappingMode> head = toProcess.poll();

	    Schema<?> schema = mapper.map(head.getFirst(), head.getSecond(), this.taskProperties.isAddXLinkedEntity(),
		    this.taskProperties.isAddXSortable(), (cls, mode) -> {
			Pair<Class<?>, ClassMappingMode> key = Pair.of(cls, mode);
			if (!queued.contains(key)) {
			    toProcess.add(key);
			    queued.add(key);
			}

			return mode.getName(ToOpenApiActionImpl.this.taskProperties, cls);
		    });

	    apiModel.schema(head.getSecond().getName(this.taskProperties, head.getFirst()), schema);
	}

	apiModel.setPaths(new PathsGenerator(isExposed, taskProperties).generate(mapper, interfaces));

	final File outputFile = new File(new URI(this.taskProperties.getOutputUri()));
	final String serialized = JacksonHelper.writeValueAsString(outputFile.getName().endsWith(".json"), apiModel);
	final byte[] jsonBytes = serialized.getBytes(StandardCharsets.UTF_8);

	Files.write(outputFile.toPath(), jsonBytes, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
		StandardOpenOption.TRUNCATE_EXISTING);

	log.info("Result ({} bytes) is written into {}", jsonBytes.length, outputFile.getPath());
    }

    private void setApiInfo(OpenAPI apiModel) {
	apiModel.setInfo(this.taskProperties.getInfo());
	if (apiModel.getInfo().getVersion() == null) {
	    apiModel.getInfo().setVersion(this.projectVersion);
	}
	if (apiModel.getInfo().getTitle() == null) {
	    apiModel.getInfo().setTitle(this.projectDisplayName + " Spring Data REST OpenAPI Specification");
	}
	if (apiModel.getInfo().getDescription() == null) {
	    apiModel.getInfo().setDescription(
		    "This is automatically generated OpenAPI Specification for Spring Data REST entities and repositories");
	}
    }

}