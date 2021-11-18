package io.github.vlsergey.springdatarestutils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import org.atteo.evo.inflector.English;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.http.HttpStatus;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import io.github.vlsergey.springdatarestutils.CodebaseScannerFacade.ScanResult;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.Components;
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

    private final @NonNull Components components;

    private final @NonNull CustomAnnotationsHelper customAnnotationsHelper;

    private final @NonNull Predicate<Class<?>> isExposed;

    private final @NonNull EntityToSchemaMapper mapper;

    private final @NonNull Paths paths;

    private final @NonNull ProjectionHelper projectionHelper;

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

    private Schema<?> buildCollectionResponseSchema(final Class<?> domainType, boolean includePageInformation) {
	final String collectionKey = English.plural(StringUtils.uncapitalize(domainType.getSimpleName()));

	final Schema<?> embeddedSchema = projectionHelper.hasProjections(domainType)
		? buildCollectionWithProjectionsSchema(domainType)
		: new ArraySchema().items(classToRefResolver.getRefSchema(domainType,
			isExposed.test(domainType) ? ClassMappingMode.WITH_LINKS : ClassMappingMode.DATA_ITEM,
			RequestType.RESPONSE));
	return EntityToSchemaMapper.buildRootCollectionSchema(taskProperties.getLinkTypeName(), collectionKey,
		embeddedSchema, includePageInformation);
    }

    public Schema<?> buildCollectionWithProjectionsSchema(@NonNull Class<?> cls) {
	final ComposedSchema oneOf = new ComposedSchema();
	oneOf.addOneOfItem(new ArraySchema()
		.items(classToRefResolver.getRefSchema(cls, ClassMappingMode.WITH_LINKS, RequestType.RESPONSE)));

	final Schema<Object> linksSchema = classToRefResolver.getRefSchema(cls, ClassMappingMode.LINKS,
		RequestType.RESPONSE);
	projectionHelper.getProjections(cls).forEach((name, projectionInterface) -> {
	    final Schema<?> projectionSchema = classToRefResolver.getRefSchema(projectionInterface,
		    ClassMappingMode.PROJECTION, RequestType.RESPONSE);
	    oneOf.addOneOfItem(new ArraySchema()
		    .items(new ComposedSchema().addAllOfItem(projectionSchema).addAllOfItem(linksSchema)));
	});

	return oneOf;
    }

    public void generate(final @NonNull Iterable<RepositoryMetadata> metas, final Set<Method> allQueryCandidates) {

	StreamSupport.stream(metas.spliterator(), false)
		.sorted(Comparator.comparing(meta -> meta.getDomainType().getName()))
		.forEach(meta -> populatePathItems(meta, allQueryCandidates));
    }

    private @NonNull String getIdPathParameterName(final @NonNull Class<?> domainType) {
	final String enitityName = StringUtils.uncapitalize(domainType.getSimpleName());
	return EntityToSchemaMapper.withBeanProperties(domainType)
		.filter(pd -> PersistenceUtils.isEmbeddedId(pd) || PersistenceUtils.isId(pd))
		.map(PropertyDescriptor::getName).findAny().orElse(enitityName + "Id");
    }

    private @NonNull Parameter getIdPathParameterRef(final @NonNull RepositoryMetadata repositoryMetadata) {
	final @NonNull Class<?> domainType = repositoryMetadata.getDomainType();
	final @NonNull String paramName = StringUtils.uncapitalize(domainType.getSimpleName()) + "Id";

	if (components.getParameters() == null || !components.getParameters().containsKey(paramName)) {
	    Schema<?> idSchema = mapper
		    .mapEntity(repositoryMetadata.getIdType(), ClassMappingMode.DATA_ITEM, RequestType.PARAMETER)
		    .nullable(false);
	    components.addParameters(paramName,
		    new Parameter().in("path").description(domainType.getSimpleName() + " identifier")
			    .name(getIdPathParameterName(domainType)).schema(idSchema));
	}

	return new Parameter().$ref("#/components/parameters/" + paramName);
    }

    private Optional<Supplier<Schema<?>>> getStandardSchemaSupplier(Class<?> cls) {
	return StandardSchemasProvider.getStandardSchemaSupplier(cls, taskProperties.isAddXJavaClassName(),
		taskProperties.isAddXJavaComparable());
    }

    public Schema<?> methodInOutsToSchema(Type type, Class<?> cls, RequestType requestType) {
	if (isExposed.test(cls)) {
	    if (requestType == RequestType.PARAMETER) {
		return getStandardSchemaSupplier(String.class).get().get();
	    }
	    return this.classToRefResolver.getRefSchema(cls, ClassMappingMode.EXPOSED, requestType);
	}

	if (Optional.class.equals(cls) && type instanceof ParameterizedType) {
	    final Type actualType = ((ParameterizedType) type).getActualTypeArguments()[0];
	    if (actualType instanceof Class<?>) {
		return methodInOutsToSchema(actualType, (Class<?>) actualType, requestType);
	    }
	}

	if (requestType == RequestType.RESPONSE) {
	    if (Collection.class.isAssignableFrom(cls) && type instanceof ParameterizedType) {
		final Type actualType = ((ParameterizedType) type).getActualTypeArguments()[0];
		if (actualType instanceof Class<?>) {
		    Class<?> actualClass = (Class<?>) actualType;
		    return buildCollectionResponseSchema(actualClass, false);
		}
	    }

	    if (Page.class.isAssignableFrom(cls) && type instanceof ParameterizedType) {
		final Type actualType = ((ParameterizedType) type).getActualTypeArguments()[0];
		if (actualType instanceof Class<?>) {
		    Class<?> actualClass = (Class<?>) actualType;
		    return buildCollectionResponseSchema(actualClass, true);
		}
	    }
	}

	if (requestType == RequestType.PARAMETER && Collection.class.isAssignableFrom(cls)
		&& type instanceof ParameterizedType) {
	    final Type actualType = ((ParameterizedType) type).getActualTypeArguments()[0];
	    if (actualType instanceof Class<?>) {
		Class<?> actualClass = (Class<?>) actualType;

		final Optional<Supplier<Schema<?>>> opSchemaSupplier = getStandardSchemaSupplier(actualClass);
		if (opSchemaSupplier.isPresent()) {
		    return new ArraySchema().items(opSchemaSupplier.get().get());
		}
	    }
	}

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
	    final @NonNull Parameter idPathParameterRef, final @NonNull Class<?> bean, final @NonNull String basePath,
	    final @NonNull Paths paths) {

	final BeanInfo beanInfo = Introspector.getBeanInfo(bean);

	// expose additional methods to get linked entity by main entity ID

	// expose one-to-one / many-to-one links
	EntityToSchemaMapper.withBeanProperties(bean, pd -> {
	    final Class<?> propertyType = pd.getPropertyType();

	    if (!isExposed.test(propertyType)) {
		return;
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
		    .addParametersItem(idPathParameterRef) //
		    .responses(withMissingResponse));

	    if (NullableUtils.getNullable(pd).orElse(true)) {
		pathItem.delete(new Operation() //
			.addTagsItem(tag) //
			.addParametersItem(idPathParameterRef) //
			.responses(new ApiResponses().addApiResponse(RESPONSE_CODE_NO_CONTENT,
				new ApiResponse().description("ok"))));
	    }

	    paths.addPathItem(basePath + "/" + pd.getName(), pathItem);
	});

	// expose one-to-many / many-to-many links
	EntityToSchemaMapper.withBeanProperties(bean, pd -> {
	    final Optional<Class<?>> opLinkedType = ReflectionUtils.getCollectionGenericTypeArgument(pd, 0)
		    .filter(isExposed);
	    if (!opLinkedType.isPresent())
		return;

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
		    .addParametersItem(idPathParameterRef) //
		    .responses(apiResponses));

	    final RequestBody urisRequestBody = new RequestBody()
		    .content(new Content().addMediaType("text/uri-list",
			    new MediaType().example("/children/42\n/children/43").schema(new StringSchema())))
		    .description("URIs pointing to the resource to bind to the association");

	    pathItem.post(new Operation() //
		    .addTagsItem(tag) //
		    .description("Binds the resource pointed to by the given URI(s) to the association resource. "
			    + "Adds specified resource to association. Returns an error if resource is already bind.") //
		    .addParametersItem(idPathParameterRef) //
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
		    .addParametersItem(idPathParameterRef) //
		    .requestBody(urisRequestBody)
		    .responses(new ApiResponses()
			    .addApiResponse(RESPONSE_CODE_NO_CONTENT, new ApiResponse().description("ok"))
			    .addApiResponse(RESPONSE_CODE_NOT_FOUND,
				    new ApiResponse().description("Entity is missing"))));

	    paths.addPathItem(basePath + "/" + pd.getName(), pathItem);

	    String linkedEntityIdParamName = bean.equals(linkedType)
		    ? "linked" + StringUtils.capitalize(getIdPathParameterName(linkedType))
		    : getIdPathParameterName(linkedType);

	    paths.addPathItem(basePath + "/" + pd.getName() + "/{" + linkedEntityIdParamName + "}",
		    new PathItem().delete(new Operation() //
			    .addTagsItem(tag) //
			    .description("Unbinds the association") //
			    .addParametersItem(idPathParameterRef) //
			    .addParametersItem(new Parameter().in("path").schema(new StringSchema().nullable(false))
				    .name(linkedEntityIdParamName).description("Assotiated entity ID"))
			    .responses(new ApiResponses().addApiResponse(RESPONSE_CODE_NO_CONTENT,
				    new ApiResponse().description("ok")))));
	});

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
	EntityToSchemaMapper.withBeanProperties(meta.getDomainType(), pd -> {
	    final Class<?> propertyType = pd.getPropertyType();

	    getStandardSchemaSupplier(propertyType).ifPresent(schemaSupplier -> {
		final Schema<?> singleItemSchema = schemaSupplier.get();
		final Schema<?> arraySchema = new ArraySchema().items(singleItemSchema).minItems(1);

		final Schema<?> oneOfSchema = new ComposedSchema().addOneOfItem(singleItemSchema)
			.addOneOfItem(arraySchema);

		final Parameter parameter = new Parameter().explode(Boolean.TRUE).in(IN_QUERY).name(pd.getName())
			.required(Boolean.FALSE).schema(oneOfSchema).style(Parameter.StyleEnum.FORM);
		operation.addParametersItem(parameter);
	    });
	});
    }

    private void populateOperationWithProjection(final @NonNull RepositoryMetadata meta,
	    final @NonNull Operation operation) {
	projectionHelper.getProjectionNames(meta.getDomainType()).ifPresent(projectionNames -> {
	    final Schema<String> schema = new StringSchema();

	    final ArrayList<String> projectionNameList = new ArrayList<>(projectionNames);
	    Collections.sort(projectionNameList);
	    schema.setEnum(projectionNameList);

	    operation.addParametersItem(new Parameter().description("The name of projection").in(IN_QUERY)
		    .name("projection").schema(schema).required(false));
	});
    }

    @SneakyThrows
    private void populatePathItems(final @NonNull RepositoryMetadata meta,
	    final @NonNull Set<Method> allQueryCandidates) {
	final Class<?> domainType = meta.getDomainType();

	final PathItem noIdPathItem = new PathItem();
	final PathItem withIdPathItem = new PathItem();

	final CrudMethods crudMethods = meta.getCrudMethods();
	final Class<?> repositoryInterface = meta.getRepositoryInterface();

	final Content entityContentWithLinks = classToRefResolver.getRefContent(domainType, ClassMappingMode.WITH_LINKS,
		RequestType.RESPONSE);

	final String tag = domainType.getSimpleName();
	final @NonNull String idPathParameterName = getIdPathParameterName(domainType);
	final @NonNull Parameter idPathParameterRef = getIdPathParameterRef(meta);
	final String collectionKey = English.plural(StringUtils.uncapitalize(domainType.getSimpleName()));
	final String basePath = "/" + collectionKey;

	final Optional<Method> opFindAllMethod = crudMethods.getFindAllMethod()
		.filter(method -> SpringDataUtils.isRestResourceExported(repositoryInterface, method));
	opFindAllMethod.ifPresent(findAllMethod -> {
	    final Operation operation = new Operation() //
		    .addTagsItem(tag).description("Find entities");

	    final Schema<?> responseSchema = buildCollectionResponseSchema(domainType,
		    Page.class.isAssignableFrom(findAllMethod.getReturnType()));

	    operation.responses(new ApiResponses().addApiResponse(RESPONSE_CODE_OK,
		    new ApiResponse().content(SchemaUtils.toContent(responseSchema)).description("Success")));

	    Optional<Method> method = ReflectionUtils.findMethod(meta.getRepositoryInterface(), findAllMethod.getName(),
		    SpringDataUtils.CLASS_QIERYDSL_PREDICATE, SpringDataUtils.CLASS_PAGEABLE);
	    if (method.isPresent()) {
		populateOperationWithPredicate(meta, operation);
		populateOperationWithPageable(operation);
		populateOperationWithProjection(meta, operation);
		customAnnotationsHelper.populateMethod(repositoryInterface, method.get(), operation);
		noIdPathItem.setGet(operation);
		return;
	    }

	    method = ReflectionUtils.findMethod(meta.getRepositoryInterface(), findAllMethod.getName(),
		    SpringDataUtils.CLASS_PAGEABLE);
	    if (method.isPresent()) {
		populateOperationWithPageable(operation);
		populateOperationWithProjection(meta, operation);
		customAnnotationsHelper.populateMethod(repositoryInterface, method.get(), operation);
		noIdPathItem.setGet(operation);
		return;
	    }
	});

	final Optional<Method> opFindOneMethod = crudMethods.getFindOneMethod()
		.filter(method -> SpringDataUtils.isRestResourceExported(repositoryInterface, method));
	opFindOneMethod.ifPresent(findOneMethod -> {
	    final Content responseContent = classToRefResolver.getRefContent(domainType,
		    projectionHelper.hasProjections(domainType) ? ClassMappingMode.WITH_PROJECTIONS
			    : ClassMappingMode.WITH_LINKS,
		    RequestType.RESPONSE);

	    final Operation getOperation = new Operation() //
		    .addTagsItem(tag) //
		    .addParametersItem(idPathParameterRef) //
		    .description("Retrieves an entity by its id") //
		    .responses(new ApiResponses()
			    .addApiResponse(RESPONSE_CODE_OK,
				    new ApiResponse().content(responseContent).description("Entity is present"))
			    .addApiResponse(RESPONSE_CODE_NOT_FOUND,
				    new ApiResponse().description("Entity is missing")));
	    customAnnotationsHelper.populateMethod(repositoryInterface, findOneMethod, getOperation);
	    populateOperationWithProjection(meta, getOperation);
	    withIdPathItem.setGet(getOperation);
	});

	final Optional<Method> opSaveMethod = crudMethods.getSaveMethod()
		.filter(method -> SpringDataUtils.isRestResourceExported(repositoryInterface, method));
	opSaveMethod.ifPresent(saveMethod -> {

	    final Operation patchOperation = new Operation() //
		    .addTagsItem(tag) //
		    .addParametersItem(idPathParameterRef) //
		    .requestBody(
			    classToRefResolver.getRequestBody(domainType, ClassMappingMode.EXPOSED, RequestType.PATCH)) //
		    .responses(new ApiResponses().addApiResponse(RESPONSE_CODE_NO_CONTENT,
			    new ApiResponse().description("Entity has been updated")));
	    customAnnotationsHelper.populateMethod(repositoryInterface, saveMethod, patchOperation);
	    withIdPathItem.setPatch(patchOperation);

	    final Operation postOperation = new Operation() //
		    .addTagsItem(tag) //
		    .requestBody(
			    classToRefResolver.getRequestBody(domainType, ClassMappingMode.EXPOSED, RequestType.CREATE)) //
		    .responses(new ApiResponses()
			    .addApiResponse(RESPONSE_CODE_OK,
				    new ApiResponse().content(entityContentWithLinks)
					    .description("Entity has been created"))
			    .addApiResponse(RESPONSE_CODE_NO_CONTENT,
				    new ApiResponse().description("Entity has been created")));
	    customAnnotationsHelper.populateMethod(repositoryInterface, saveMethod, postOperation);
	    noIdPathItem.setPost(postOperation);

	    final Operation putOperation = new Operation() //
		    .addTagsItem(tag) //
		    .addParametersItem(idPathParameterRef) //
		    .requestBody(
			    classToRefResolver.getRequestBody(domainType, ClassMappingMode.EXPOSED, RequestType.UPDATE)) //
		    .responses(new ApiResponses().addApiResponse(RESPONSE_CODE_NO_CONTENT,
			    new ApiResponse().description("Entity has been updated")));
	    customAnnotationsHelper.populateMethod(repositoryInterface, saveMethod, putOperation);
	    withIdPathItem.setPut(putOperation);
	});

	final Optional<Method> opDeleteMethod = crudMethods.getDeleteMethod()
		.filter(method -> SpringDataUtils.isRestResourceExported(repositoryInterface, method));
	opDeleteMethod.ifPresent(deleteMethod -> {
	    final Operation deleteOperation = new Operation() //
		    .addTagsItem(tag) //
		    .addParametersItem(idPathParameterRef) //
		    .description("Deletes the entity with the given id") //
		    .responses(new ApiResponses().addApiResponse(RESPONSE_CODE_NO_CONTENT,
			    new ApiResponse().description("Entity has been deleted or already didn't exists")));
	    customAnnotationsHelper.populateMethod(repositoryInterface, deleteMethod, deleteOperation);
	    withIdPathItem.setDelete(deleteOperation);
	});

	if (!noIdPathItem.readOperations().isEmpty()) {
	    paths.addPathItem(basePath, noIdPathItem);
	}

	if (!withIdPathItem.readOperations().isEmpty()) {
	    paths.addPathItem(basePath + "/{" + idPathParameterName + "}", withIdPathItem);
	}

	crudMethods.getFindOneMethod().ifPresent(findOneMethod ->
	// expose additional methods to get linked entity by main entity ID
	populateAssociationResourceMethods(tag, idPathParameterRef, domainType,
		basePath + "/{" + idPathParameterName + "}", paths));

	populatePathItemsWithSearchQueries(meta, allQueryCandidates, tag, basePath);
    }

    private void populatePathItemsWithSearchQueries(final RepositoryMetadata meta, final Set<Method> allQueryCandidates,
	    final String tag, final String basePath) {
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
		final Operation operation = new Operation().addTagsItem(tag)
			.responses(new ApiResponses().addApiResponse(RESPONSE_CODE_OK,
				new ApiResponse().description("ok").content(
					SchemaUtils.toContent(methodInOutsToSchema(method.getGenericReturnType(),
						method.getReturnType(), RequestType.RESPONSE)))));

		for (java.lang.reflect.Parameter methodParam : method.getParameters()) {
		    operation.addParametersItem(
			    new Parameter().in(IN_QUERY).name(methodParam.getName()).schema(methodInOutsToSchema(
				    methodParam.getParameterizedType(), methodParam.getType(), RequestType.PARAMETER)));
		}

		customAnnotationsHelper.populateMethod(repoInterface, method, operation);
		paths.addPathItem(basePath + "/search" + path.toString(), new PathItem().get(operation));

	    } catch (UnsupportedOperationException exc) {
		log.warn("Unable to generate mapping to method " + method + ": " + exc.getMessage());
	    }
	}
    }

}
