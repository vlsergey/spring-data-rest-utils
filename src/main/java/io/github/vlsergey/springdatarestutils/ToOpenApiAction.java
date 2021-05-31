package io.github.vlsergey.springdatarestutils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import org.atteo.evo.inflector.English;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.util.Pair;
import org.springframework.util.StringUtils;

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
class ToOpenApiAction implements Action<Task> {

    private final PluginProperties pluginProperties;
    private final Project project;

    public ToOpenApiAction(Project project, PluginProperties pluginProperties) {
	this.project = project;
	this.pluginProperties = pluginProperties;
    }

    @Override
    public void execute(Task taskImpl) {
	final List<URL> urls = new ArrayList<>();

	SourceSetContainer sourceSetContainer = project.getConvention().getByType(SourceSetContainer.class);
	sourceSetContainer.getAsMap().forEach((String name, SourceSet set) -> {
	    final Consumer<? super File> addToClassPath = file -> {
		try {
		    final URL url = file.toURI().toURL();
		    urls.add(url);
		    log.debug("Added to classPath: {}", url);
		} catch (MalformedURLException e) {
		    e.printStackTrace();
		}
	    };

	    set.getCompileClasspath().forEach(addToClassPath);
	    set.getOutput().getClassesDirs().forEach(addToClassPath);
	});

	try (URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(URL[]::new),
		Thread.currentThread().getContextClassLoader())) {

	    executeWithClassLoader(urlClassLoader);
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    @SneakyThrows
    void executeWithClassLoader(ClassLoader urlClassLoader) {
	final RepositoryEnumerator repositoryEnumerator = new RepositoryEnumerator(
		this.pluginProperties.getBasePackage().get(),
		this.pluginProperties.getRepositoryDetectionStrategyEnum().get());

	final Set<RepositoryMetadata> interfaces = repositoryEnumerator.enumerate(urlClassLoader);

	final EntityToSchemaMapper mapper = new EntityToSchemaMapper(interfaces);

	Queue<Pair<Class<?>, ClassMappingMode>> toProcess = new LinkedList<>();
	Set<Pair<Class<?>, ClassMappingMode>> queued = new HashSet<>();

	OpenAPI apiModel = new OpenAPI();
	setApiInfo(apiModel);
	apiModel.setServers(pluginProperties.getServers().get());

	interfaces.forEach(meta -> {
	    Pair<Class<?>, ClassMappingMode> key = Pair.of(meta.getDomainType(), ClassMappingMode.TOP_LEVEL_ENTITY);
	    toProcess.add(key);
	    queued.add(key);
	});

	while (!toProcess.isEmpty()) {
	    final Pair<Class<?>, ClassMappingMode> head = toProcess.poll();

	    Schema<?> schema = mapper.map(head.getFirst(), head.getSecond(),
		    pluginProperties.getAddXLinkedEntity().get(), pluginProperties.getAddXSortable().get(),
		    (cls, mode) -> {
			Pair<Class<?>, ClassMappingMode> key = Pair.of(cls, mode);
			if (!queued.contains(key)) {
			    toProcess.add(key);
			    queued.add(key);
			}

			return mode.getName(ToOpenApiAction.this.pluginProperties, cls);
		    });

	    apiModel.schema(head.getSecond().getName(pluginProperties, head.getFirst()), schema);
	}

	Paths paths = new Paths();
	apiModel.setPaths(paths);
	// pathes
	interfaces.forEach(meta -> {
	    Schema<?> idSchema = mapper.map(meta.getIdType(), ClassMappingMode.DATA_ITEM, false, false,
		    (cls, mode) -> mode.getName(ToOpenApiAction.this.pluginProperties, cls));

	    populatePathItems(cls -> ClassMappingMode.SECOND_LEVEL.getName(ToOpenApiAction.this.pluginProperties, cls),
		    meta, idSchema, paths);
	});

	final RegularFileProperty outputProperty = this.pluginProperties.getOutput();
	final File outputFile = outputProperty.getAsFile().get();
	final String serialized = JacksonHelper.writeValueAsString(outputFile.getName().endsWith(".json"), apiModel);
	final byte[] jsonBytes = serialized.getBytes(StandardCharsets.UTF_8);

	final RegularFile output = pluginProperties.getOutput().get();
	Files.write(output.getAsFile().toPath(), jsonBytes, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
		StandardOpenOption.TRUNCATE_EXISTING);

	log.info("Result ({} bytes) is written into {}", jsonBytes.length, output.getAsFile().getPath());
    }

    private void populatePathItems(Function<Class<?>, String> nameResolver, RepositoryMetadata meta, Schema<?> idSchema,
	    Paths paths) {
	final Class<?> domainType = meta.getDomainType();

	final PathItem noIdPathItem = new PathItem();
	final PathItem withIdPathItem = new PathItem();

	final CrudMethods crudMethods = meta.getCrudMethods();

	crudMethods.getSaveMethod().ifPresent(saveMethod -> {
	    final Schema<Object> schema = new Schema<>().$ref("#/components/schemas/" + nameResolver.apply(domainType));
	    final MediaType mediaType = new MediaType().schema(schema);
	    final Content content = new Content().addMediaType("application/json", mediaType);
	    final RequestBody requestBody = new RequestBody().required(Boolean.TRUE).content(content);
	    noIdPathItem.setPost(new Operation() //
		    .addTagsItem(domainType.getSimpleName()).requestBody(requestBody) //
		    .responses(new ApiResponses()
			    .addApiResponse("200",
				    new ApiResponse().content(content).description("Entity has been created"))
			    .addApiResponse("204", new ApiResponse().description("Entity has been created"))));

	    withIdPathItem.setPut(new Operation() //
		    .addTagsItem(domainType.getSimpleName())
		    .addParametersItem(
			    new Parameter().allowEmptyValue(Boolean.FALSE).in("path").schema(idSchema).name("id")) //
		    .requestBody(requestBody) //
		    .responses(new ApiResponses().addApiResponse("204",
			    new ApiResponse().description("Entity has been updated"))));
	});

	crudMethods.getDeleteMethod().ifPresent(deleteMethod -> {
	    withIdPathItem.setDelete(new Operation() //
		    .addTagsItem(domainType.getSimpleName()) //
		    .addParametersItem(new Parameter().in("path").schema(idSchema).name("id")) //
		    .responses(new ApiResponses().addApiResponse("204",
			    new ApiResponse().description("Entity has been deleted or already didn't exists"))));
	});

	final String basePath = "/" + English.plural(StringUtils.uncapitalize(domainType.getSimpleName()));
	if (!noIdPathItem.readOperations().isEmpty()) {
	    paths.addPathItem(basePath, noIdPathItem);
	}

	if (!withIdPathItem.readOperations().isEmpty()) {
	    paths.addPathItem(basePath + "/{id}", withIdPathItem);
	}
    }

    private void setApiInfo(OpenAPI apiModel) {
	apiModel.setInfo(pluginProperties.getInfo().get());
	if (apiModel.getInfo().getVersion() == null) {
	    apiModel.getInfo().setVersion(project.getVersion().toString());
	}
	if (apiModel.getInfo().getTitle() == null) {
	    apiModel.getInfo().setTitle(project.getDisplayName() + " Spring Data REST OpenAPI Specification");
	}
	if (apiModel.getInfo().getDescription() == null) {
	    apiModel.getInfo().setDescription(
		    "This is automatically generated OpenAPI Specification for Spring Data REST entities and repositories");
	}
    }

}