package io.github.vlsergey.springdatarestutils;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;
import org.springframework.data.util.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.vlsergey.springdatarestutils.CodebaseScannerFacade.ScanResult;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
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
	final CodebaseScannerFacade scannerFacade = new CodebaseScannerFacade(this.taskProperties.getBasePackage(),
		RepositoryDetectionStrategies.valueOf(this.taskProperties.getRepositoryDetectionStrategy()));

	final ScanResult scanResult = scannerFacade.scan(Thread.currentThread().getContextClassLoader());
	final Predicate<Class<?>> isExposed = cls -> scanResult.getRepositories().stream()
		.anyMatch(meta -> meta.getDomainType().isAssignableFrom(cls));

	final EntityToSchemaMapper mapper = new EntityToSchemaMapper(isExposed, taskProperties);

	Queue<Pair<Class<?>, ClassMappingMode>> toProcess = new LinkedList<>();
	Set<Pair<Class<?>, ClassMappingMode>> queued = new HashSet<>();

	OpenAPI apiModel = new OpenAPI();
	setApiInfo(apiModel);
	apiModel.setServers(this.taskProperties.getServers());

	Consumer<Class<?>> onProjection = projectionClass -> {
	    final Pair<Class<?>, ClassMappingMode> projectionKey = Pair.of(projectionClass,
		    ClassMappingMode.PROJECTION);
	    if (!queued.contains(projectionKey)) {
		toProcess.add(projectionKey);
		queued.add(projectionKey);
	    }
	};

	scanResult.getRepositories().forEach(meta -> {
	    final Pair<Class<?>, ClassMappingMode> entityKey = Pair.of(meta.getDomainType(), ClassMappingMode.EXPOSED);
	    toProcess.add(entityKey);
	    queued.add(entityKey);

	    final Pair<Class<?>, ClassMappingMode> entityLinksKey = Pair.of(meta.getDomainType(),
		    ClassMappingMode.LINKS);
	    toProcess.add(entityLinksKey);
	    queued.add(entityLinksKey);

	    RepositoryRestResource resAnn = meta.getRepositoryInterface().getAnnotation(RepositoryRestResource.class);
	    if (resAnn != null) {
		final Class<?> projectionClass = resAnn.excerptProjection();
		if (projectionClass != null) {
		    onProjection.accept(projectionClass);
		}
	    }
	});
	scanResult.getProjections().stream().forEach(onProjection);

	while (!toProcess.isEmpty()) {
	    final Pair<Class<?>, ClassMappingMode> head = toProcess.poll();

	    Schema<?> schema = mapper.mapEntity(head.getFirst(), head.getSecond(), (cls, mode) -> {
		Pair<Class<?>, ClassMappingMode> key = Pair.of(cls, mode);
		if (!queued.contains(key)) {
		    toProcess.add(key);
		    queued.add(key);
		}

		return mode.getName(ToOpenApiActionImpl.this.taskProperties, cls);
	    });

	    apiModel.schema(head.getSecond().getName(this.taskProperties, head.getFirst()), schema);
	}

	apiModel.setPaths(new PathsGenerator(isExposed, taskProperties).generate(mapper, scanResult.getRepositories()));

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