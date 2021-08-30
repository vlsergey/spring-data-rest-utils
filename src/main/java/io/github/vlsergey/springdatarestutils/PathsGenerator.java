package io.github.vlsergey.springdatarestutils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
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
import org.springframework.data.rest.core.config.Projection;
import org.springframework.http.HttpStatus;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import static java.util.stream.Collectors.toList;

import io.github.vlsergey.springdatarestutils.CodebaseScannerFacade.ScanResult;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.*;
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

    private static final String IN_QUERY = ParameterIn.QUERY.toString();

    private static final String RESPONSE_CODE_CONFLICT = String.valueOf(HttpStatus.CONFLICT.value());
    private static final String RESPONSE_CODE_NO_CONTENT = String.valueOf(HttpStatus.NO_CONTENT.value());
    private static final String RESPONSE_CODE_NOT_FOUND = String.valueOf(HttpStatus.NOT_FOUND.value());
    private static final String RESPONSE_CODE_OK = String.valueOf(HttpStatus.OK.value());

    private final ClassToRefResolver classToRefResolver;

    private final @NonNull Predicate<Class<?>> isExposed;

    private final @NonNull ScanResult scanResult;

    private final @NonNull TaskProperties taskProperties;

    private static boolean isBaseClassMethod(Class<?> repoInterface, Method method) {
	if (SpringDataUtils.CLASS_QIERYDSL_PREDICATE_EXECUTOR.<Method>flatMap(cls -> cls.isAssignableFrom(repoInterface)
		? ReflectionUtils.findMethod(cls, method.getName(), method.getParameterTypes())
		: Optional.<Method>empty()).isPresent()) {
	    return true;
	}

	return SpringDataUtils.CLASS_SIMPLE_JPA_REPOSITORY.map(cls -> org.springframework.util.ReflectionUtils
		.findMethod(cls, method.getName(), method.getParameterTypes())).isPresent();
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

    private Optional<Supplier<Schema<?>>> getStandardSchemaSupplier(Class<?> cls) {
	return StandardSchemasProvider.getStandardSchemaSupplier(cls, taskProperties.isAddXJavaClassName(),
		taskProperties.isAddXJavaComparable());
    }

    public Schema<?> methodInOutsToSchema(Class<?> cls) {
	final Optional<Supplier<Schema<?>>> schema = getStandardSchemaSupplier(cls);
	if (!schema.isPresent()) {
	    throw new UnsupportedOperationException("Unsupported class: " + cls.getName());
	}
	return schema.get().get();
    }

    // https://docs.spring.io/spring-data/rest/docs/current/reference/html/#repository-resources.association-resource
    @SneakyThrows
    private void populateAssociationResourceMethods(final @NonNull String tag,
	    // TODO: move to components
	    final @NonNull Parameter mainIdParameter, final @NonNull Class<?> bean, final @NonNull String basePath,
	    final @NonNull Paths paths) {

	final BeanInfo beanInfo = Introspector.getBeanInfo(bean);

	// expose additional methods to get linked entity by main entity ID

	// expose one-to-one / many-to-one links
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
	    final ApiResponses withMissingResponse = new ApiResponses().addApiResponse(RESPONSE_CODE_OK, okResponse)
		    .addApiResponse(RESPONSE_CODE_NOT_FOUND, new ApiResponse().description("Entity is missing"));

	    pathItem.get(new Operation() //
		    .addTagsItem(tag) //
		    .addParametersItem(mainIdParameter) //
		    .responses(withMissingResponse));

	    if (NullableUtils.getNullable(pd).orElse(true)) {
		pathItem.delete(new Operation() //
			.addTagsItem(tag) //
			.addParametersItem(mainIdParameter) //
			.responses(new ApiResponses().addApiResponse(RESPONSE_CODE_NO_CONTENT,
				new ApiResponse().description("ok"))));
	    }

	    paths.addPathItem(basePath + "/" + pd.getName(), pathItem);
	}

	// expose one-to-many / many-to-many links
	for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
	    final Optional<Class<?>> opLinkedType = ReflectionUtils.getCollectionGenericTypeArgument(pd)
		    .filter(isExposed);
	    if (!opLinkedType.isPresent())
		continue;

	    final Class<?> linkedType = opLinkedType.get();
	    final PathItem pathItem = new PathItem();

	    final String collectionKey = English.plural(StringUtils.uncapitalize(linkedType.getSimpleName()));

	    final Schema<?> responseSchema = EntityToSchemaMapper.buildOneToManyCollectionSchema(
		    taskProperties.getLinkTypeName(), collectionKey,
		    classToRefResolver.getRefSchema(linkedType, ClassMappingMode.WITH_LINKS, RequestType.RESPONSE));

	    final ApiResponses apiResponses = new ApiResponses();

	    apiResponses.addApiResponse(RESPONSE_CODE_OK,
		    new ApiResponse().content(SchemaUtils.toContent(responseSchema)).description("Success"));

	    apiResponses.addApiResponse(RESPONSE_CODE_NOT_FOUND, new ApiResponse().description("Entity is missing"));

	    pathItem.get(new Operation() //
		    .addTagsItem(tag) //
		    .addParametersItem(mainIdParameter) //
		    .responses(apiResponses));

	    final RequestBody urisRequestBody = new RequestBody()
		    .content(new Content().addMediaType("text/uri-list",
			    new MediaType().example("/children/42\n/children/43").schema(new StringSchema())))
		    .description("URIs pointing to the resource to bind to the association");

	    pathItem.post(new Operation() //
		    .addTagsItem(tag) //
		    .description("Binds the resource pointed to by the given URI(s) to the association resource. "
			    + "Adds specified resource to association. Returns an error if resource is already bind.") //
		    .addParametersItem(mainIdParameter) //
		    .requestBody(urisRequestBody)
		    .responses(new ApiResponses()
			    .addApiResponse(RESPONSE_CODE_NO_CONTENT, new ApiResponse().description("ok"))
			    .addApiResponse(RESPONSE_CODE_NOT_FOUND, new ApiResponse().description("Entity is missing"))
			    .addApiResponse(RESPONSE_CODE_CONFLICT, new ApiResponse()
				    .description("Problems like key duplication or unique constrain violation"))));

	    pathItem.put(new Operation() //
		    .addTagsItem(tag) //
		    .description("Binds the resource pointed to by the given URI(s) to the association resource. "
			    + "Overwrites existing assotiation.") //
		    .addParametersItem(mainIdParameter) //
		    .requestBody(urisRequestBody)
		    .responses(new ApiResponses()
			    .addApiResponse(RESPONSE_CODE_NO_CONTENT, new ApiResponse().description("ok"))
			    .addApiResponse(RESPONSE_CODE_NOT_FOUND,
				    new ApiResponse().description("Entity is missing"))));

	    paths.addPathItem(basePath + "/" + pd.getName(), pathItem);

	    paths.addPathItem(basePath + "/" + pd.getName() + "/{childId}", new PathItem().delete(new Operation() //
		    .addTagsItem(tag) //
		    .description("Unbinds the association") //
		    .addParametersItem(mainIdParameter) //
		    .addParametersItem(new Parameter().in("path").schema(new StringSchema().nullable(false))
			    .name("childId").description("Assotiated entity ID"))
		    .responses(new ApiResponses().addApiResponse(RESPONSE_CODE_NO_CONTENT,
			    new ApiResponse().description("ok")))));
	}

	/*
	 * There is no need to go deeper -- Spring Data REST can't handle deeper links
	 */
    }

    @SneakyThrows
    private void populateOperationWithPageable(Operation operation) {
	operation.addParametersItem(new Parameter().description("Results page you want to retrieve (0..N)").in(IN_QUERY)
		.name("page").schema(new IntegerSchema().nullable(Boolean.FALSE).minimum(BigDecimal.ZERO)));

	operation.addParametersItem(new Parameter().description("Number of records per page").in(IN_QUERY).name("size")
		.schema(new IntegerSchema().nullable(Boolean.FALSE).minimum(BigDecimal.ONE)
			.maximum(BigDecimal.valueOf(100))));

	operation.addParametersItem(new Parameter().description("Sorting parameters").explode(Boolean.TRUE).in(IN_QUERY)
		.name("sort").schema(new ArraySchema().items(new StringSchema())).style(StyleEnum.FORM));
    }

    @SneakyThrows
    private void populateOperationWithPredicate(@NonNull RepositoryMetadata meta, final @NonNull Operation operation) {
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

    private void populateOperationWithProjection(final @NonNull RepositoryMetadata meta,
	    final @NonNull Operation operation) {
	final List<Class<?>> projections = this.scanResult.getProjections().stream().filter(
		cls -> Arrays.asList(cls.getAnnotation(Projection.class).types()).contains(meta.getDomainType()))
		.collect(toList());
	if (projections.isEmpty())
	    return;

	final Schema<String> schema = new StringSchema();
	schema.setEnum(projections.stream()
		.map(cls -> Optional.ofNullable(
			org.apache.commons.lang3.StringUtils.trimToNull(cls.getAnnotation(Projection.class).name()))
			.orElseGet(cls::getSimpleName))
		.collect(toList()));
	operation.addParametersItem(new Parameter().description("The name of projection").in(IN_QUERY)
		.name("projection").schema(schema).required(false));
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
		    SpringDataUtils.CLASS_QIERYDSL_PREDICATE, SpringDataUtils.CLASS_PAGEABLE);
	    if (method.isPresent()) {
		populateOperationWithPredicate(meta, operation);
		populateOperationWithPageable(operation);
		populateOperationWithProjection(meta, operation);
		noIdPathItem.setGet(operation);
		return;
	    }

	    method = ReflectionUtils.findMethod(meta.getRepositoryInterface(), findAllMethod.getName(),
		    SpringDataUtils.CLASS_PAGEABLE);
	    if (method.isPresent()) {
		populateOperationWithPageable(operation);
		populateOperationWithProjection(meta, operation);
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

	    noIdPathItem.setPost(new Operation() //
		    .addTagsItem(tag) //
		    .requestBody(
			    classToRefResolver.getRequestBody(domainType, ClassMappingMode.EXPOSED, RequestType.CREATE)) //
		    .responses(new ApiResponses()
			    .addApiResponse(RESPONSE_CODE_OK,
				    new ApiResponse().content(entityContentWithLinks)
					    .description("Entity has been created"))
			    .addApiResponse(RESPONSE_CODE_NO_CONTENT,
				    new ApiResponse().description("Entity has been created"))));

	    withIdPathItem.setPut(new Operation() //
		    .addTagsItem(tag) //
		    .addParametersItem(idParameter) //
		    .requestBody(
			    classToRefResolver.getRequestBody(domainType, ClassMappingMode.EXPOSED, RequestType.UPDATE)) //
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
	populateAssociationResourceMethods(tag, idParameter, domainType, basePath + "/{id}", paths));

	populatePathItemsWithSearchQueries(meta, allQueryCandidates, paths, tag, basePath);
    }

    private void populatePathItemsWithSearchQueries(final RepositoryMetadata meta, final Set<Method> allQueryCandidates,
	    final Paths paths, final String tag, final String basePath) {
	final Class<?> repoInterface = meta.getRepositoryInterface();
	for (Method method : repoInterface.getMethods()) {
	    method = ClassUtils.getMostSpecificMethod(method, repoInterface);
	    if (!allQueryCandidates.contains(method) || isBaseClassMethod(repoInterface, method)) {
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
