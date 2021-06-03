package io.github.vlsergey.springdatarestutils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.function.Predicate;

import org.atteo.evo.inflector.English;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.http.HttpStatus;
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

	final Schema<Object> entitySchemaRef = new Schema<>()
		.$ref("#/components/schemas/" + ClassMappingMode.EXPOSED_NO_LINKS.getName(taskProperties, domainType));
	final MediaType entityMediaType = new MediaType().schema(entitySchemaRef);
	final Content entityContent = new Content().addMediaType(APPLICATION_JSON_VALUE, entityMediaType);

	final Schema<Object> entityWithLinksSchemaRef = new Schema<>().$ref(
		"#/components/schemas/" + ClassMappingMode.EXPOSED_WITH_LINKS.getName(taskProperties, domainType));
	final MediaType entityWithLinksMediaType = new MediaType().schema(entityWithLinksSchemaRef);
	final Content entityWithLinksContent = new Content().addMediaType(APPLICATION_JSON_VALUE,
		entityWithLinksMediaType);

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
					    new ApiResponse().content(entityWithLinksContent)
						    .description("Entity is present"))
				    .addApiResponse(RESPONSE_CODE_NOT_FOUND,
					    new ApiResponse().description("Entity is missing"))));

	    // expose additional methods to get linked entity by main entity ID
	    populatePathItemsDeep(tag, idParameter, domainType, basePath + "/{id}", 1, paths);
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

	if (!noIdPathItem.readOperations().isEmpty()) {
	    paths.addPathItem(basePath, noIdPathItem);
	}

	if (!withIdPathItem.readOperations().isEmpty()) {
	    paths.addPathItem(basePath + "/{id}", withIdPathItem);
	}
    }

    @SneakyThrows
    void populatePathItemsDeep(final @NonNull String tag,
	    // TODO: move to components
	    final @NonNull Parameter mainIdParameter, final @NonNull Class<?> bean, final @NonNull String basePath,
	    int currentDepth, final @NonNull Paths paths) {
	if (currentDepth > this.taskProperties.getLinkDepth()) {
	    return;
	}

	final BeanInfo beanInfo = Introspector.getBeanInfo(bean);

	// expose additional methods to get linked entity by main entity ID
	for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
	    final Class<?> propertyType = pd.getPropertyType();
	    if (!isExposed.test(propertyType)) {
		continue;
	    }

	    // TODO: move to components
	    final ApiResponse okResponse = new ApiResponse()
		    .content(new Content().addMediaType(APPLICATION_JSON_VALUE,
			    new MediaType().schema(new Schema<>().$ref("#/components/schemas/"
				    + ClassMappingMode.EXPOSED_WITH_LINKS.getName(taskProperties, propertyType)))))
		    .description("Entity is present");

	    // TODO: move to components
	    final ApiResponses missingResponse = new ApiResponses().addApiResponse(RESPONSE_CODE_OK, okResponse)
		    .addApiResponse(RESPONSE_CODE_NOT_FOUND, new ApiResponse().description("Entity is missing"));

	    final String componentPath = basePath + "/" + pd.getName();

	    paths.addPathItem(componentPath, new PathItem().get((new Operation() //
		    .addTagsItem(tag) //
		    .addParametersItem(mainIdParameter) //
		    .responses(missingResponse))));

	    // we need to go deeper...
	    populatePathItemsDeep(tag, mainIdParameter, propertyType, componentPath, currentDepth + 1, paths);
	}

    }

}
