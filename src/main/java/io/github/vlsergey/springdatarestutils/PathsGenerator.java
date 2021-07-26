package io.github.vlsergey.springdatarestutils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.atteo.evo.inflector.English;
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
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.Parameter.StyleEnum;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class PathsGenerator {

    private static final String CLASSNAME_QUERYDSL_PREDICATE = "com.querydsl.core.types.Predicate";
    private static final String CLASSNAME_SPRING_PAGEABLE = "org.springframework.data.domain.Pageable";

    private static final String IN_QUERY = "query";

    private static final String RESPONSE_CODE_NO_CONTENT = String.valueOf(HttpStatus.NO_CONTENT.value());
    private static final String RESPONSE_CODE_NOT_FOUND = String.valueOf(HttpStatus.NOT_FOUND.value());
    private static final String RESPONSE_CODE_OK = String.valueOf(HttpStatus.OK.value());

    private final ClassToRefResolver classToRefResolver;

    private final @NonNull Predicate<Class<?>> isExposed;

    private final @NonNull TaskProperties taskProperties;

    private static boolean isNullable(PropertyDescriptor pd) {
	return PersistenceUtils.getJoinColumnNullable(pd).or(() -> PersistenceUtils.getColumnNullable(pd)).orElse(true);
    }

    public Paths generate(final @NonNull EntityToSchemaMapper mapper, final @NonNull Iterable<RepositoryMetadata> metas,
	    final Set<Method> allQueryCandidates) {
	Paths paths = new Paths();
	metas.forEach(meta -> {
	    Schema<?> idSchema = mapper.mapEntity(meta.getIdType(), ClassMappingMode.DATA_ITEM, RequestType.PARAMETER);
	    populatePathItems(meta, allQueryCandidates, idSchema, paths);
	});
	return paths;
    }

    @SuppressWarnings("rawtypes")
    private Optional<Supplier<Schema>> getStandardSchemaSupplier(Class<?> cls) {
	return StandardSchemasProvider.getStandardSchemaSupplier(cls, taskProperties.isAddXJavaClassName(),
		taskProperties.isAddXJavaComparable());
    }

    public Schema<?> methodInOutsToSchema(Class<?> cls) {
	@SuppressWarnings("rawtypes")
	final Optional<Supplier<Schema>> schema = getStandardSchemaSupplier(cls);
	if (!schema.isPresent()) {
	    throw new UnsupportedOperationException("Unsupported class: " + cls.getName());
	}
	return schema.get().get();
    }

    @SneakyThrows
    private void populateOperationWithPageable(Operation operation) {
	final Supplier<Schema> intSchemaSupplier = getStandardSchemaSupplier(int.class).get();

	operation.addParametersItem(new Parameter().description("Results page you want to retrieve (0..N)").in(IN_QUERY)
		.name("page").schema(intSchemaSupplier.get().minimum(BigDecimal.ZERO)));

	operation.addParametersItem(new Parameter().description("Number of records per page").in(IN_QUERY).name("size")
		.schema(intSchemaSupplier.get().minimum(BigDecimal.ONE).maximum(BigDecimal.valueOf(100))));

	operation.addParametersItem(new Parameter().description("Sorting parameters").explode(Boolean.TRUE).in(IN_QUERY)
		.name("sort").schema(new ArraySchema().items(new StringSchema())).style(StyleEnum.FORM));
    }

    @SneakyThrows
    private void populateOperationWithPredicate(@NonNull RepositoryMetadata meta, Operation operation) {
	final Class<?> cls = meta.getDomainType();
	final BeanInfo beanInfo = Introspector.getBeanInfo(cls);
	for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
	    if (pd.getReadMethod().getDeclaringClass().getName().startsWith("java.lang.")) {
		continue;
	    }
	    final Class<?> propertyType = pd.getPropertyType();

	    getStandardSchemaSupplier(propertyType).ifPresent(schemaSupplier -> {
		operation.addParametersItem(
			new Parameter().in(IN_QUERY).name(pd.getName()).schema(schemaSupplier.get()));
	    });
	}
    }

    @SneakyThrows
    public void populatePathItems(final @NonNull RepositoryMetadata meta, final Set<Method> allQueryCandidates,
	    final @NonNull Schema<?> idSchema, final @NonNull Paths paths) {
	final Class<?> domainType = meta.getDomainType();

	final PathItem noIdPathItem = new PathItem();
	final PathItem withIdPathItem = new PathItem();

	final CrudMethods crudMethods = meta.getCrudMethods();

	final Content entityContentWithLinks = classToRefResolver.getRefContent(domainType, ClassMappingMode.WITH_LINKS,
		RequestType.RESPONSE);

	final String tag = domainType.getSimpleName();
	final Parameter idParameter = new Parameter().in("path").schema(idSchema).name("id").description("Entity ID");
	final String collectionKey = English.plural(StringUtils.uncapitalize(domainType.getSimpleName()));
	final String basePath = "/" + collectionKey;

	crudMethods.getFindAllMethod().ifPresent(findAllMethod -> {
	    final Operation operation = new Operation() //
		    .addTagsItem(tag).description("Find entities");

	    final Schema<?> responseSchema = EntityToSchemaMapper.buildRootCollectionSchema(
		    taskProperties.getLinkTypeName(), collectionKey,
		    classToRefResolver.getRefSchema(domainType, ClassMappingMode.WITH_LINKS, RequestType.RESPONSE));

	    operation.responses(new ApiResponses().addApiResponse(RESPONSE_CODE_OK,
		    new ApiResponse().content(SchemaUtils.toContent(responseSchema)).description("Success")));

	    Optional<Method> method = ReflectionUtils.findMethod(meta.getRepositoryInterface(), findAllMethod.getName(),
		    CLASSNAME_QUERYDSL_PREDICATE, CLASSNAME_SPRING_PAGEABLE);
	    if (method.isPresent()) {
		populateOperationWithPredicate(meta, operation);
		populateOperationWithPageable(operation);
		noIdPathItem.setGet(operation);
		return;
	    }

	    method = ReflectionUtils.findMethod(meta.getRepositoryInterface(), findAllMethod.getName(),
		    CLASSNAME_SPRING_PAGEABLE);
	    if (method.isPresent()) {
		populateOperationWithPageable(operation);
		noIdPathItem.setGet(operation);
		return;
	    }
	});

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
	});

	crudMethods.getSaveMethod().ifPresent(saveMethod -> {

	    withIdPathItem.setPatch(new Operation() //
		    .addTagsItem(tag) //
		    .addParametersItem(idParameter) //
		    .requestBody(
			    classToRefResolver.getRequestBody(domainType, ClassMappingMode.EXPOSED, RequestType.PATCH)) //
		    .responses(new ApiResponses().addApiResponse(RESPONSE_CODE_NO_CONTENT,
			    new ApiResponse().description("Entity has been updated"))));

	    final RequestBody requestBody = classToRefResolver.getRequestBody(domainType, ClassMappingMode.EXPOSED,
		    RequestType.CREATE_OR_UPDATE);

	    noIdPathItem.setPost(new Operation() //
		    .addTagsItem(tag) //
		    .requestBody(requestBody) //
		    .responses(new ApiResponses()
			    .addApiResponse(RESPONSE_CODE_OK,
				    new ApiResponse().content(entityContentWithLinks)
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

	crudMethods.getFindOneMethod().ifPresent(findOneMethod ->
	// expose additional methods to get linked entity by main entity ID
	populatePathItemsDeeper(tag, idParameter, domainType, basePath + "/{id}", paths));

	populatePathItemsWithSearchQueries(meta, allQueryCandidates, paths, tag, basePath);
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
	    final PathItem pathItem = new PathItem();

	    // TODO: move to components
	    final ApiResponse okResponse = new ApiResponse().content(
		    classToRefResolver.getRefContent(propertyType, ClassMappingMode.EXPOSED, RequestType.RESPONSE))
		    .description("Entity is present");

	    // TODO: move to components
	    final ApiResponses missingResponse = new ApiResponses().addApiResponse(RESPONSE_CODE_OK, okResponse)
		    .addApiResponse(RESPONSE_CODE_NOT_FOUND, new ApiResponse().description("Entity is missing"));

	    pathItem.get(new Operation() //
		    .addTagsItem(tag) //
		    .addParametersItem(mainIdParameter) //
		    .responses(missingResponse));

	    if (isNullable(pd)) {
		pathItem.delete(new Operation() //
			.addTagsItem(tag) //
			.addParametersItem(mainIdParameter) //
			.responses(new ApiResponses().addApiResponse(RESPONSE_CODE_NO_CONTENT,
				new ApiResponse().description("ok"))));
	    }

	    paths.addPathItem(basePath + "/" + pd.getName(), pathItem);

	    /*
	     * There is no need to go deeper -- Spring Data REST can't handle deeper links
	     */
	}

    }

    private void populatePathItemsWithSearchQueries(final RepositoryMetadata meta, final Set<Method> allQueryCandidates,
	    final Paths paths, final String tag, final String basePath) {
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
		final Operation operation = new Operation().addTagsItem(tag).responses(
			new ApiResponses().addApiResponse(RESPONSE_CODE_OK, new ApiResponse().description("ok")
				.content(SchemaUtils.toContent(methodInOutsToSchema(method.getReturnType())))));

		for (java.lang.reflect.Parameter methodParam : method.getParameters()) {
		    operation.addParametersItem(new Parameter().in(IN_QUERY).name(methodParam.getName())
			    .schema(methodInOutsToSchema(methodParam.getType())));
		}

		paths.addPathItem(basePath + "/search" + path.toString(), new PathItem().get(operation));

	    } catch (UnsupportedOperationException exc) {
		log.warn("Unable to generate mapping to method " + method + ": " + exc.getMessage());
	    }
	}
    }

}
