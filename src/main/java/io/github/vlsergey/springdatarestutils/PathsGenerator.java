package io.github.vlsergey.springdatarestutils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.atteo.evo.inflector.English;
import org.gradle.internal.impldep.com.esotericsoftware.minlog.Log;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.http.HttpStatus;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

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
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

@AllArgsConstructor
public class PathsGenerator {

    private static final String APPLICATION_JSON_VALUE = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

    private static final String RESPONSE_CODE_NO_CONTENT = String.valueOf(HttpStatus.NO_CONTENT.value());
    private static final String RESPONSE_CODE_NOT_FOUND = String.valueOf(HttpStatus.NOT_FOUND.value());
    private static final String RESPONSE_CODE_OK = String.valueOf(HttpStatus.OK.value());

    private final @NonNull Predicate<Class<?>> isExposed;

    private final @NonNull TaskProperties taskProperties;

    private static Content toContent(Schema<?> schema) {
	final MediaType mediaType = new MediaType().schema(schema);
	return new Content().addMediaType(APPLICATION_JSON_VALUE, mediaType);
    }

    private @NonNull Schema<Object> buildRefSchema(final @NonNull Class<?> cls, final @NonNull ClassMappingMode mode) {
	return new Schema<>().$ref("#/components/schemas/" + mode.getName(taskProperties, cls));
    }

    public Paths generate(final @NonNull EntityToSchemaMapper mapper, final @NonNull Iterable<RepositoryMetadata> metas,
	    final Set<Method> allQueryCandidates) {
	Paths paths = new Paths();
	metas.forEach(meta -> {
	    Schema<?> idSchema = mapper.mapEntity(meta.getIdType(), ClassMappingMode.DATA_ITEM,
		    (cls, mode) -> mode.getName(taskProperties, cls));

	    populatePathItems(meta, allQueryCandidates, idSchema, paths);
	});

	return paths;
    }

    public Schema<?> methodInOutsToSchema(Class<?> cls) {
	@SuppressWarnings("rawtypes")
	final Optional<Supplier<Schema>> schema = StandardSchemasProvider.getStandardSchemaSupplier(cls, false);
	if (!schema.isPresent()) {
	    throw new UnsupportedOperationException("Unsupported class: " + cls.getName());
	}
	return schema.get().get();
    }

    @SneakyThrows
    public void populatePathItems(final @NonNull RepositoryMetadata meta, final Set<Method> allQueryCandidates,
	    final @NonNull Schema<?> idSchema, final @NonNull Paths paths) {
	final Class<?> domainType = meta.getDomainType();

	final PathItem noIdPathItem = new PathItem();
	final PathItem withIdPathItem = new PathItem();

	final CrudMethods crudMethods = meta.getCrudMethods();

	final Content entityContent = toContent(buildRefSchema(domainType, ClassMappingMode.EXPOSED));
	final Content entityContentWithLinks = toContent(buildRefSchema(domainType, ClassMappingMode.WITH_LINKS));

	final String tag = domainType.getSimpleName();
	final Parameter idParameter = new Parameter().in("path").schema(idSchema).name("id").description("Entity ID");
	final String basePath = "/" + English.plural(StringUtils.uncapitalize(domainType.getSimpleName()));

	crudMethods.getFindOneMethod().ifPresent(findOneMethod -> {
	    withIdPathItem.setGet(new Operation() //
		    .addTagsItem(tag) //
		    .addParametersItem(idParameter) //
		    .description("Retrieves an entity by its id") //
		    .responses(
			    new ApiResponses()
				    .addApiResponse(RESPONSE_CODE_OK,
					    new ApiResponse().content(entityContentWithLinks)
						    .description("Entity is present"))
				    .addApiResponse(RESPONSE_CODE_NOT_FOUND,
					    new ApiResponse().description("Entity is missing"))));

	    // expose additional methods to get linked entity by main entity ID
	    populatePathItemsDeeper(tag, idParameter, domainType, basePath + "/{id}", paths);
	});

	crudMethods.getSaveMethod().ifPresent(saveMethod -> {
	    final RequestBody requestBody = new RequestBody().required(Boolean.TRUE).content(entityContent);
	    noIdPathItem.setPost(new Operation() //
		    .addTagsItem(tag) //
		    .requestBody(requestBody) //
		    .responses(new ApiResponses()
			    .addApiResponse(RESPONSE_CODE_OK,
				    new ApiResponse().content(entityContentWithLinks)
					    .description("Entity has been created"))
			    .addApiResponse(RESPONSE_CODE_NO_CONTENT,
				    new ApiResponse().description("Entity has been created"))));

	    withIdPathItem.setPatch(new Operation() //
		    .addTagsItem(tag) //
		    .addParametersItem(idParameter) //
		    .requestBody(new RequestBody().required(Boolean.TRUE)
			    .content(toContent(buildRefSchema(domainType, ClassMappingMode.EXPOSED_PATCH)))) //
		    .responses(new ApiResponses().addApiResponse(RESPONSE_CODE_NO_CONTENT,
			    new ApiResponse().description("Entity has been updated"))));

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

	for (Method method : meta.getRepositoryInterface().getMethods()) {
	    method = ClassUtils.getMostSpecificMethod(method, meta.getRepositoryInterface());
	    if (!allQueryCandidates.contains(method)) {
		continue;
	    }

	    try {
		RestResource annotation = AnnotationUtils.findAnnotation(method, RestResource.class);
		Path path = annotation == null || !StringUtils.hasText(annotation.path()) ? new Path(method.getName())
			: new Path(annotation.path());

		// TODO: check void
		final Operation operation = new Operation().addTagsItem(tag)
			.responses(new ApiResponses().addApiResponse(RESPONSE_CODE_OK, new ApiResponse()
				.description("ok").content(toContent(methodInOutsToSchema(method.getReturnType())))));

		for (java.lang.reflect.Parameter methodParam : method.getParameters()) {
		    operation.addParametersItem(new Parameter().in("query").name(methodParam.getName())
			    .schema(methodInOutsToSchema(methodParam.getType())));
		}

		paths.addPathItem(basePath + path.toString(), new PathItem().get(operation));

	    } catch (UnsupportedOperationException exc) {
		Log.warn("Unable to generate mapping to method " + method + ": " + exc.getMessage());
	    }
	}

	if (!noIdPathItem.readOperations().isEmpty()) {
	    paths.addPathItem(basePath, noIdPathItem);
	}

	if (!withIdPathItem.readOperations().isEmpty()) {
	    paths.addPathItem(basePath + "/{id}", withIdPathItem);
	}
    }

    @SneakyThrows
    void populatePathItemsDeeper(final @NonNull String tag,
	    // TODO: move to components
	    final @NonNull Parameter mainIdParameter, final @NonNull Class<?> bean, final @NonNull String basePath,
	    final @NonNull Paths paths) {

	final BeanInfo beanInfo = Introspector.getBeanInfo(bean);

	// expose additional methods to get linked entity by main entity ID
	for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
	    final Class<?> propertyType = pd.getPropertyType();
	    if (!isExposed.test(propertyType)) {
		continue;
	    }

	    // TODO: move to components
	    final ApiResponse okResponse = new ApiResponse()
		    .content(toContent(buildRefSchema(propertyType, ClassMappingMode.EXPOSED)))
		    .description("Entity is present");

	    // TODO: move to components
	    final ApiResponses missingResponse = new ApiResponses().addApiResponse(RESPONSE_CODE_OK, okResponse)
		    .addApiResponse(RESPONSE_CODE_NOT_FOUND, new ApiResponse().description("Entity is missing"));

	    final String componentPath = basePath + "/" + pd.getName();

	    paths.addPathItem(componentPath, new PathItem().get((new Operation() //
		    .addTagsItem(tag) //
		    .addParametersItem(mainIdParameter) //
		    .responses(missingResponse))));

	    /*
	     * There is no need to go deeper -- Spring Data REST can't handle deeper links
	     */
	}

    }

}
