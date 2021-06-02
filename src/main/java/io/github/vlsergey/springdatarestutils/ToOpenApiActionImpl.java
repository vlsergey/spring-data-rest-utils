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
import java.util.function.BiFunction;

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

    private static final String APPLICATION_JSON_VALUE = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

    private static final String RESPONSE_CODE_OK = String.valueOf(HttpStatus.OK.value());
    private static final String RESPONSE_CODE_NO_CONTENT = String.valueOf(HttpStatus.NO_CONTENT.value());
    private static final String RESPONSE_CODE_NOT_FOUND = String.valueOf(HttpStatus.NOT_FOUND.value());

    private final TaskProperties taskProperties;
    private final String projectDisplayName;
    private final String projectVersion;

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

	final EntityToSchemaMapper mapper = new EntityToSchemaMapper(interfaces);

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

	Paths paths = new Paths();
	apiModel.setPaths(paths);
	// pathes
	interfaces.forEach(meta -> {
	    Schema<?> idSchema = mapper.map(meta.getIdType(), ClassMappingMode.DATA_ITEM, false, false,
		    (cls, mode) -> mode.getName(ToOpenApiActionImpl.this.taskProperties, cls));

	    populatePathItems((cls, mode) -> mode.getName(ToOpenApiActionImpl.this.taskProperties, cls), meta, idSchema,
		    paths);
	});

	final File outputFile = new File(new URI(this.taskProperties.getOutputUri()));
	final String serialized = JacksonHelper.writeValueAsString(outputFile.getName().endsWith(".json"), apiModel);
	final byte[] jsonBytes = serialized.getBytes(StandardCharsets.UTF_8);

	Files.write(outputFile.toPath(), jsonBytes, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
		StandardOpenOption.TRUNCATE_EXISTING);

	log.info("Result ({} bytes) is written into {}", jsonBytes.length, outputFile.getPath());
    }

    private void populatePathItems(BiFunction<Class<?>, ClassMappingMode, String> nameResolver, RepositoryMetadata meta,
	    Schema<?> idSchema, Paths paths) {
	final Class<?> domainType = meta.getDomainType();

	final PathItem noIdPathItem = new PathItem();
	final PathItem withIdPathItem = new PathItem();

	final CrudMethods crudMethods = meta.getCrudMethods();

	final Schema<Object> entitySchemaRef = new Schema<>()
		.$ref("#/components/schemas/" + nameResolver.apply(domainType, ClassMappingMode.EXPOSED_NO_LINKS));
	final MediaType entityMediaType = new MediaType().schema(entitySchemaRef);
	final Content entityContent = new Content().addMediaType(APPLICATION_JSON_VALUE, entityMediaType);

	final Schema<Object> entityWithLinksSchemaRef = new Schema<>()
		.$ref("#/components/schemas/" + nameResolver.apply(domainType, ClassMappingMode.EXPOSED_WITH_LINKS));
	final MediaType entityWithLinksMediaType = new MediaType().schema(entityWithLinksSchemaRef);
	final Content entityWithLinksContent = new Content().addMediaType(APPLICATION_JSON_VALUE,
		entityWithLinksMediaType);

	final String tag = domainType.getSimpleName();
	final Parameter idParameter = new Parameter().in("path").schema(idSchema).name("id").description("Entity ID");

	crudMethods.getFindOneMethod().ifPresent(findOneMethod -> {
	    withIdPathItem.setGet(new Operation() //
		    .addTagsItem(tag) //
		    .addParametersItem(idParameter) //
		    .description("Retrieves an entity by its id") //
		    .responses(
			    new ApiResponses()
				    .addApiResponse(RESPONSE_CODE_OK,
					    new ApiResponse().content(entityWithLinksContent)
						    .description("Entity is present"))
				    .addApiResponse(RESPONSE_CODE_NOT_FOUND,
					    new ApiResponse().description("Entity is missing"))));
	});

	crudMethods.getSaveMethod().ifPresent(saveMethod -> {
	    final RequestBody requestBody = new RequestBody().required(Boolean.TRUE).content(entityContent);
	    noIdPathItem.setPost(new Operation() //
		    .addTagsItem(tag) //
		    .requestBody(requestBody) //
		    .responses(new ApiResponses()
			    .addApiResponse(RESPONSE_CODE_OK,
				    new ApiResponse().content(entityWithLinksContent)
					    .description("Entity has been created"))
			    .addApiResponse(RESPONSE_CODE_NO_CONTENT,
				    new ApiResponse().description("Entity has been created"))));

	    withIdPathItem.setPut(new Operation() //
		    .addTagsItem(tag) //
		    .addParametersItem(idParameter) //
		    .requestBody(requestBody) //
		    .responses(new ApiResponses().addApiResponse(RESPONSE_CODE_NO_CONTENT,
			    new ApiResponse().description("Entity has been updated"))));
	});

	crudMethods.getDeleteMethod().ifPresent(deleteMethod -> {
	    withIdPathItem.setDelete(new Operation() //
		    .addTagsItem(tag) //
		    .addParametersItem(idParameter) //
		    .description("Deletes the entity with the given id") //
		    .responses(new ApiResponses().addApiResponse(RESPONSE_CODE_NO_CONTENT,
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