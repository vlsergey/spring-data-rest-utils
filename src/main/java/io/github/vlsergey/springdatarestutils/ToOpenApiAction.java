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

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFile;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.util.Pair;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
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

	    final RepositoryEnumerator repositoryEnumerator = new RepositoryEnumerator(
		    this.pluginProperties.getBasePackage().get(),
		    this.pluginProperties.repositoryDetectionStrategyEnum.get());

	    final Set<RepositoryMetadata> interfaces = repositoryEnumerator.enumerate(urlClassLoader);

	    final EntityToSchemaMapper mapper = new EntityToSchemaMapper(interfaces);

	    Queue<Pair<Class<?>, ClassMappingMode>> toProcess = new LinkedList<>();
	    Set<Pair<Class<?>, ClassMappingMode>> queued = new HashSet<>();

	    OpenAPI apiModel = new OpenAPI();
	    setApiInfo(apiModel);

	    apiModel.setPaths(new Paths()); // empty so far

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

	    final String serialized = JacksonHelper.writeValueAsString(
		    this.pluginProperties.getOutput().getAsFile().get().getName().endsWith(".json"), apiModel);
	    final byte[] jsonBytes = serialized.getBytes(StandardCharsets.UTF_8);

	    final RegularFile output = pluginProperties.getOutput().get();
	    Files.write(output.getAsFile().toPath(), jsonBytes, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
		    StandardOpenOption.TRUNCATE_EXISTING);

	    log.info("Result ({} bytes) is written into {}", jsonBytes.length, output.getAsFile().getPath());
	} catch (IOException e) {
	    e.printStackTrace();
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