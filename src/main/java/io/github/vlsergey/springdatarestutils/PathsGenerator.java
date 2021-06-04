package io.github.vlsergey.springdatarestutils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.function.Predicate;

import org.atteo.evo.inflector.English;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ComposedSchema;
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

    public Paths generate(final @NonNull EntityToSchemaMapper mapper,
	    final @NonNull Iterable<RepositoryMetadata> metas) {
	Paths paths = new Paths();
	metas.forEach(meta -> {
	    Schema<?> idSchema = mapper.map(meta.getIdType(), ClassMappingMode.DATA_ITEM, false, false,
		    (cls, mode) -> mode.getName(taskProperties, cls));

	    populatePathItems(meta, idSchema, paths);
	});

	return paths;
    }

    @SneakyThrows
    public void populatePathItems(final @NonNull RepositoryMetadata meta, final @NonNull Schema<?> idSchema,
	    final @NonNull Paths paths) {
	final Class<?> domainType = meta.getDomainType();

	final PathItem noIdPathItem = new PathItem();
	final PathItem withIdPathItem = new PathItem();

	final CrudMethods crudMethods = meta.getCrudMethods();

	final Content entityContent = toContent(buildRefSchema(domainType, ClassMappingMode.EXPOSED_NO_LINKS));

	final Content standardResponseContent;
	final RepositoryRestResource repoRestResAnn = meta.getRepositoryInterface()
		.getAnnotation(RepositoryRestResource.class);
	if (repoRestResAnn != null && repoRestResAnn.excerptProjection() != null) {
	    final Schema<Object> entityWithLinksSchemaRef = buildRefSchema(domainType,
		    ClassMappingMode.EXPOSED_WITH_LINKS);
	    final Schema<Object> projectionSchemaRef = buildRefSchema(repoRestResAnn.excerptProjection(),
		    ClassMappingMode.PROJECTION);
	    final Schema<Object> allOfSchema = new ComposedSchema().addAllOfItem(entityWithLinksSchemaRef)
		    .addAllOfItem(projectionSchemaRef);

	    standardResponseContent = toContent(allOfSchema);
	} else {
	    standardResponseContent = toContent(buildRefSchema(domainType, ClassMappingMode.EXPOSED_WITH_LINKS));
	}

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
					    new ApiResponse().content(standardResponseContent)
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
				    new ApiResponse().content(standardResponseContent)
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
		    .content(toContent(buildRefSchema(propertyType, ClassMappingMode.EXPOSED_WITH_LINKS)))
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
