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

import org.apache.commons.lang3.tuple.Triple;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.vlsergey.springdatarestutils.CodebaseScannerFacade.ScanResult;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import lombok.NonNull;
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

	Queue<Triple<Class<?>, ClassMappingMode, RequestType>> toProcess = new LinkedList<>();
	Set<Triple<Class<?>, ClassMappingMode, RequestType>> queued = new HashSet<>();

	OpenAPI apiModel = new OpenAPI();
	setApiInfo(apiModel);
	apiModel.setComponents(new Components());
	apiModel.setPaths(new Paths());
	apiModel.setServers(this.taskProperties.getServers());

	Consumer<Class<?>> onProjection = projectionClass -> {
	    final Triple<Class<?>, ClassMappingMode, RequestType> projectionKey = Triple.of(projectionClass,
		    ClassMappingMode.PROJECTION, RequestType.RESPONSE);
	    if (!queued.contains(projectionKey)) {
		toProcess.add(projectionKey);
		queued.add(projectionKey);
	    }
	};

	final ClassToRefResolver classToRefResolver = (@NonNull Class<?> cls,
		@NonNull ClassMappingMode classMappingMode, @NonNull RequestType requestType) -> {

	    final Triple<Class<?>, ClassMappingMode, RequestType> key = Triple.of(cls, classMappingMode, requestType);
	    if (!queued.contains(key)) {
		toProcess.add(key);
		queued.add(key);
	    }

	    return ClassToRefResolver.generateName(taskProperties, cls, classMappingMode, requestType);
	};

	final CustomAnnotationsHelper customAnnotationsHelper = new CustomAnnotationsHelper(taskProperties);

	final EntityToSchemaMapper mapper = new EntityToSchemaMapper(classToRefResolver, customAnnotationsHelper,
		isExposed, scanResult, taskProperties);

	final PathsGenerator pathsGenerator = new PathsGenerator(classToRefResolver, apiModel.getComponents(),
		customAnnotationsHelper, isExposed, mapper, apiModel.getPaths(), scanResult, taskProperties);
	pathsGenerator.generate(scanResult.getRepositories(), scanResult.getQueryMethodsCandidates());

	scanResult.getRepositories().forEach(meta -> {
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
	    final Triple<Class<?>, ClassMappingMode, RequestType> head = toProcess.poll();

	    final Class<?> cls = head.getLeft();
	    final ClassMappingMode mode = head.getMiddle();
	    final RequestType requestType = head.getRight();

	    Schema<?> schema = mapper.mapEntity(cls, mode, requestType);
	    apiModel.schema(ClassToRefResolver.generateName(this.taskProperties, cls, mode, requestType), schema);
	}

	SchemaUtils.sortMapByKeys(apiModel.getComponents().getSchemas());
	SchemaUtils.sortMapByKeys(apiModel.getPaths());

	final File outputFile = new File(new URI(this.taskProperties.getOutputUri()));
	final String serialized = SchemaUtils.writeValueAsString(outputFile.getName().endsWith(".json"), apiModel);
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