package io.github.vlsergey.springdatarestutils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.hateoas.Link;
import org.springframework.util.StringUtils;

import io.github.vlsergey.springdatarestutils.CodebaseScannerFacade.ScanResult;
import io.swagger.v3.oas.models.media.*;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

@AllArgsConstructor
public class EntityToSchemaMapper {

    private static final Class<Object> ANY_VALUE_CLASS = Object.class;

    private final @NonNull ClassToRefResolver classToRefResolver;

    private final @NonNull CustomAnnotationsHelper customAnnotationsHelper;

    private final @NonNull Predicate<@NonNull Class<?>> isExposed;

    private final @NonNull ProjectionHelper projectionHelper;

    private final @NonNull ScanResult scanResult;

    private final @NonNull TaskProperties taskProperties;

    @SuppressWarnings("rawtypes")
    static Schema<?> buildOneToManyCollectionSchema(String linkTypeName, String collectionKey, Schema itemRefSchema) {
	ObjectSchema result = new ObjectSchema();
	result.setRequired(Arrays.asList("_embedded", "_links", "page"));

	result.addProperties("_embedded", new ObjectSchema().addRequiredItem(collectionKey).addProperties(collectionKey,
		new ArraySchema().items(itemRefSchema)));

	final Schema<?> linkSchema = new Schema<>().$ref("#/components/schemas/" + linkTypeName);

	result.addProperties("_links", new ObjectSchema().addRequiredItem("self").addProperties("self", linkSchema));

	return result;
    }

    @SuppressWarnings("rawtypes")
    static Schema<?> buildRootCollectionSchema(String linkTypeName, String collectionKey, Schema<?> embeddedArraySchema,
	    boolean includePageInformation) {
	ObjectSchema result = new ObjectSchema();
	result.setRequired(Arrays.asList("_embedded", "_links", "page"));

	result.addProperties("_embedded",
		new ObjectSchema().addRequiredItem(collectionKey).addProperties(collectionKey, embeddedArraySchema));

	final Schema<?> linkSchema = new Schema<>().$ref("#/components/schemas/" + linkTypeName);

	result.addProperties("_links",
		new ObjectSchema().addRequiredItem("self").addRequiredItem("profile").addRequiredItem("search")
			.addProperties("self", linkSchema).addProperties("profile", linkSchema)
			.addProperties("search", linkSchema));

	if (includePageInformation) {
	    Schema int32NonNegativeSchema = new IntegerSchema().format("int32").minimum(BigDecimal.ZERO)
		    .nullable(Boolean.FALSE);
	    Schema int64NonNegativeSchema = new IntegerSchema().format("int64").minimum(BigDecimal.ZERO)
		    .nullable(Boolean.FALSE);

	    result.addProperties("page",
		    new ObjectSchema().addRequiredItem("size").addRequiredItem("totalElements")
			    .addRequiredItem("totalPages").addRequiredItem("number")
			    .addProperties("size", int32NonNegativeSchema)
			    .addProperties("totalElements", int64NonNegativeSchema)
			    .addProperties("totalPages", int32NonNegativeSchema)
			    .addProperties("number", int32NonNegativeSchema));
	}

	return result;
    }

    private static boolean isGeneratedValue(PropertyDescriptor pd) {
	return PersistenceUtils.isGeneratedValue(pd) || HibernateUtils.isCreationTimestamp(pd)
		|| HibernateUtils.isUpdateTimestamp(pd);
    }

    private static Schema<?> makeNullable(Schema<?> schema) {
	ComposedSchema composedSchema = new ComposedSchema();
	composedSchema.setNullable(Boolean.TRUE);
	composedSchema.addOneOfItem(schema);
	return composedSchema;
    }

    @SneakyThrows
    static Stream<PropertyDescriptor> withBeanProperties(Class<?> cls) {
	final BeanInfo beanInfo = Introspector.getBeanInfo(cls);
	return Arrays.stream(beanInfo.getPropertyDescriptors())
		.filter(pd -> !pd.getReadMethod().getDeclaringClass().getName().startsWith("java.lang."))
		.filter(pd -> !PersistenceUtils.isTransient(pd));
    }

    static void withBeanProperties(Class<?> cls, Consumer<PropertyDescriptor> consumer) {
	if (!cls.isInterface()) {
	    withBeanProperties(cls).forEach(consumer);
	    return;
	}
	// special handling for interfaces
	withBeanPropertiesImpl(cls, consumer, new HashSet<>());
    }

    static void withBeanPropertiesImpl(final @NonNull Class<?> interfaceClass,
	    final @NonNull Consumer<PropertyDescriptor> consumer, final @NonNull Set<String> alreadyProcessed) {
	withBeanProperties(interfaceClass).filter(pd -> !alreadyProcessed.contains(pd.getName())).forEach(pd -> {
	    alreadyProcessed.add(pd.getName());
	    consumer.accept(pd);
	});
	for (Class<?> superInterface : interfaceClass.getInterfaces()) {
	    withBeanPropertiesImpl(superInterface, consumer, alreadyProcessed);
	}
    }

    @SneakyThrows
    private Schema<?> buildEntityLinksSchema(final @NonNull Class<?> cls, final @NonNull ClassMappingMode mode,
	    final @NonNull RequestType requestType) {
	final Map<String, Class<?>> links = new TreeMap<>();
	links.put("self", cls);
	links.put(StringUtils.uncapitalize(cls.getSimpleName()), cls);

	withBeanProperties(cls, pd -> {
	    final Class<?> propertyType = pd.getPropertyType();
	    if (Collection.class.isAssignableFrom(propertyType)) {
		// not yet implemented
		return;
	    }
	    if (!isExposed.test(propertyType)) {
		return;
	    }
	    links.put(pd.getName(), propertyType);
	});

	ObjectSchema linksSchema = new ObjectSchema();
	linksSchema.setProperties(new TreeMap<>());
	links.forEach((key, linkClass) -> {
	    final Schema<?> refSchema = classToRefResolver.getRefSchema(Link.class, ClassMappingMode.DATA_ITEM,
		    requestType);
	    if (!taskProperties.isAddXJavaClassName() && !taskProperties.isAddXLinkedEntity()) {
		linksSchema.getProperties().put(key, refSchema);
		return;
	    }

	    ComposedSchema composedSchema = new ComposedSchema();
	    composedSchema.addAllOfItem(refSchema);
	    if (taskProperties.isAddXLinkedEntity()) {
		composedSchema.addExtension(ExtensionConstants.X_LINKED_ENTITY,
			classToRefResolver.getRefName(linkClass, ClassMappingMode.EXPOSED, requestType));
	    }
	    if (taskProperties.isAddXJavaClassName()) {
		composedSchema.addExtension(ExtensionConstants.X_JAVA_CLASS_NAME, linkClass.getName());
	    }
	    linksSchema.getProperties().put(key, composedSchema);
	});

	final ObjectSchema objectSchema = new ObjectSchema();
	objectSchema.setName(classToRefResolver.getRefName(cls, mode, requestType));
	objectSchema.addRequiredItem("_links");
	objectSchema.addProperties("_links", linksSchema);
	return objectSchema;
    }

    private @NonNull ComposedSchema buildHierarchyRootSchema(final @NonNull Class<?> cls,
	    final @NonNull RequestType requestType) {
	ComposedSchema composedSchema = new ComposedSchema();

	String discriminatorColumnName = PersistenceUtils.getDiscriminatorColumnName(cls).orElse("DTYPE");
	Optional<String> discriminatorFieldName = withBeanProperties(cls)
		.filter(pd -> PersistenceUtils.getColumnName(pd).equalsIgnoreCase(discriminatorColumnName))
		.map(PropertyDescriptor::getName).findAny();

	discriminatorFieldName
		.ifPresent(fieldName -> composedSchema.setDiscriminator(new Discriminator().propertyName(fieldName)));

	scanResult.getInheritance().get(cls).forEach(childClass -> {
	    final ClassMappingMode childClassMode = childClass.equals(cls) ? ClassMappingMode.INHERITANCE_BASE
		    : ClassMappingMode.INHERITANCE_CHILD;

	    PersistenceUtils.getDiscriminatorValue(childClass).ifPresent(discriminatorValue -> {
		if (composedSchema.getDiscriminator() != null) {
		    composedSchema.getDiscriminator().mapping(discriminatorValue,
			    classToRefResolver.getRefUrl(childClass, childClassMode, requestType));
		}
	    });

	    composedSchema.addAnyOfItem(classToRefResolver.getRefSchema(childClass, childClassMode, requestType));
	});

	return composedSchema;
    }

    @SneakyThrows
    private Schema<?> buildWithLinksSchema(final @NonNull Class<?> cls) {
	final @NonNull Schema<?> withoutLinks = classToRefResolver.getRefSchema(cls, ClassMappingMode.EXPOSED,
		RequestType.RESPONSE);
	final @NonNull Schema<?> links = classToRefResolver.getRefSchema(cls, ClassMappingMode.LINKS,
		RequestType.RESPONSE);

	return new ComposedSchema().addAllOfItem(withoutLinks).addAllOfItem(links);
    }

    private Schema<?> buildWithProjections(@NonNull Class<?> cls) {
	final @NonNull Schema<?> withoutProjections = classToRefResolver.getRefSchema(cls, ClassMappingMode.EXPOSED,
		RequestType.RESPONSE);

	final ComposedSchema oneOf = new ComposedSchema().addOneOfItem(withoutProjections);
	projectionHelper.getProjections(cls).forEach((name, projectionInterface) -> {
	    final Schema<?> projectionSchema = classToRefResolver.getRefSchema(projectionInterface,
		    ClassMappingMode.PROJECTION, RequestType.RESPONSE);
	    oneOf.addOneOfItem(projectionSchema);
	});

	final @NonNull Schema<?> links = classToRefResolver.getRefSchema(cls, ClassMappingMode.LINKS,
		RequestType.RESPONSE);

	return new ComposedSchema().addAllOfItem(oneOf).addAllOfItem(links);
    }

    @SneakyThrows
    public Schema<?> mapEntity(final @NonNull Class<?> cls, final @NonNull ClassMappingMode mode,
	    final @NonNull RequestType requestType) {
	if (mode == ClassMappingMode.LINKS) {
	    return buildEntityLinksSchema(cls, mode, requestType);
	}

	if (mode == ClassMappingMode.WITH_LINKS) {
	    return buildWithLinksSchema(cls);
	}

	if (mode == ClassMappingMode.WITH_PROJECTIONS) {
	    return buildWithProjections(cls);
	}

	if (mode == ClassMappingMode.INHERITANCE_CHILD) {
	    ComposedSchema composedSchema = new ComposedSchema();
	    final Class<?> baseClass = scanResult.getInheritance().entrySet().stream()
		    .filter(e -> e.getValue().contains(cls)).findAny().get().getKey();
	    composedSchema.addAllOfItem(
		    classToRefResolver.getRefSchema(baseClass, ClassMappingMode.INHERITANCE_BASE, requestType));
	    composedSchema.addAllOfItem(toObjectSchema(mode, requestType, cls));
	    return composedSchema;
	}

	if (cls.isEnum()) {
	    return mapEnum((Class) cls);
	}

	final Optional<Supplier<Schema<?>>> entityStandardSchemaSupplier = StandardSchemasProvider
		.getStandardSchemaSupplier(cls, taskProperties.isAddXJavaClassName(),
			taskProperties.isAddXJavaComparable());
	if (entityStandardSchemaSupplier.isPresent()) {
	    return entityStandardSchemaSupplier.get().get();
	}

	if (mode == ClassMappingMode.EXPOSED && requestType == RequestType.RESPONSE
		&& this.scanResult.getInheritance().containsKey(cls)) {
	    return buildHierarchyRootSchema(cls, requestType);
	}

	return toObjectSchema(mode, requestType, cls);
    }

    private <T extends Enum<T>> StringSchema mapEnum(Class<T> cls) {
	final StringSchema stringSchema = new StringSchema();
	for (T constant : cls.getEnumConstants()) {
	    stringSchema.addEnumItem(constant.name());
	}
	return stringSchema;
    }

    private void populateSchema(PropertyDescriptor pd, Schema<?> schema) {
	ValidationUtils.getMaxValue(pd).ifPresent(value -> schema.setMaximum(BigDecimal.valueOf(value)));
	ValidationUtils.getMinValue(pd).ifPresent(value -> schema.setMinimum(BigDecimal.valueOf(value)));

	if (String.class.equals(pd.getPropertyType())) {
	    PersistenceUtils.getColumnLength(pd).ifPresent(schema::setMaxLength);
	    HibernateValidatorUtils.getLengthMin(pd).ifPresent(schema::setMinLength);
	    HibernateValidatorUtils.getLengthMax(pd).ifPresent(schema::setMaxLength);
	}

	customAnnotationsHelper.populatePropertySchema(pd, schema);
    }

    private ObjectSchema toObjectSchema(final @NonNull ClassMappingMode mode, final @NonNull RequestType requestType,
	    final @NonNull Class<?> cls) {
	final ObjectSchema objectSchema = new ObjectSchema();
	final String refName = classToRefResolver.getRefName(cls, mode, requestType);
	objectSchema.setName(refName);

	withBeanProperties(cls, pd -> {
	    final Class<?> propertyType = pd.getPropertyType();
	    final boolean isCollection = propertyType.isArray() || Collection.class.isAssignableFrom(propertyType)
		    || Map.class.isAssignableFrom(propertyType);

	    final Optional<Boolean> nullable = isCollection && requestType != RequestType.RESPONSE
		    ? Optional.of(Boolean.TRUE)
		    : NullableUtils.getNullable(pd);

	    if (mode == ClassMappingMode.INHERITANCE_CHILD && !pd.getReadMethod().getDeclaringClass().equals(cls)) {
		return;
	    }

	    if (isExposed.test(propertyType)
		    || ReflectionUtils.getCollectionGenericTypeArgument(pd, 0).filter(isExposed).isPresent()) {
		switch (mode) {
		case PROJECTION:
		case DATA_ITEM:
		    break;
		default:
		    if (requestType == RequestType.RESPONSE) {
			// exposed as link only
			return;
		    }
		}
	    }

	    if (requestType != RequestType.PARAMETER && JacksonUtils.isJsonIgnore(pd)) {
		return;
	    }
	    if (requestType == RequestType.CREATE && (!PersistenceUtils.isColumnInsertable(pd)
		    || !PersistenceUtils.isJoinColumnInsertable(pd) || HibernateUtils.isCreationTimestamp(pd))) {
		return;
	    }
	    if ((requestType == RequestType.UPDATE || requestType == RequestType.PATCH)
		    && (!PersistenceUtils.isColumnUpdatable(pd) || !PersistenceUtils.isJoinColumnUpdatable(pd)
			    || HibernateUtils.isUpdateTimestamp(pd))) {
		return;
	    }
	    if (requestType != RequestType.RESPONSE && HibernateUtils.isFormula(pd)) {
		return;
	    }

	    switch (requestType) {
	    case PATCH:
		// never add as required
		break;
	    case RESPONSE:
		if (JacksonUtils.nullIncludedInJson(cls).orElse(false)) {
		    // field will always present in response, but may be null
		    objectSchema.addRequiredItem(pd.getName());
		}
		break;
	    case CREATE:
	    case UPDATE:
		if (!nullable.orElse(false) && !isGeneratedValue(pd) && !HibernateUtils.isFormula(pd))
		    objectSchema.addRequiredItem(pd.getName());
		break;
	    }

	    final Optional<Boolean> dstNullable;
	    switch (requestType) {
	    case RESPONSE:
		dstNullable = OptionalUtils.allTrue(nullable, Optional.of(!PersistenceUtils.isEmbeddedId(pd)),
			Optional.of(!isGeneratedValue(pd)), Optional.of(!PersistenceUtils.isId(pd)),
			JacksonUtils.nullIncludedInJson(cls), Optional.of(!isCollection));
		break;
	    case CREATE:
	    case UPDATE:
		dstNullable = nullable;
		break;
	    default:
		dstNullable = Optional.empty();
		break;
	    }

	    Schema<?> schema = toSchema(mode, requestType, Optional.of(pd), propertyType, dstNullable);
	    if (schema.get$ref() == null) {
		populateSchema(pd, schema);
	    }
	    objectSchema.addProperties(pd.getName(), schema);
	});
	return objectSchema;
    }

    public Schema<?> toSchema(final @NonNull ClassMappingMode mode, final @NonNull RequestType requestType,
	    final Optional<PropertyDescriptor> opPd, final @NonNull Class<?> propertyType,
	    final @NonNull Optional<Boolean> nullable) {
	if (propertyType.isEnum()) {
	    return classToRefResolver.getRefSchema(propertyType, ClassMappingMode.DATA_ITEM, requestType);
	}

	if ((mode != ClassMappingMode.PROJECTION && mode != ClassMappingMode.DATA_ITEM)
		&& isExposed.test(propertyType)) {
	    StringSchema schema = new StringSchema();
	    nullable.ifPresent(schema::setNullable);
	    return schema;
	}

	if (propertyType.isArray()) {
	    ArraySchema schema = new ArraySchema();
	    schema.setItems(toSchema(mode, requestType, Optional.empty(), propertyType.getComponentType(),
		    requestType == RequestType.RESPONSE ? Optional.of(Boolean.FALSE) : Optional.empty()));
	    nullable.ifPresent(schema::setNullable);
	    return schema;
	}

	if (Collection.class.isAssignableFrom(propertyType)) {
	    final @NonNull Optional<Class<?>> itemType = OptionalUtils.coalesce(
		    opPd.flatMap(PersistenceUtils::getElementCollectionTargetClass),
		    opPd.flatMap(pd -> ReflectionUtils.getCollectionGenericTypeArgument(pd, 0)),
		    Optional.of(ANY_VALUE_CLASS));
	    if (itemType.isPresent()) {
		final ArraySchema schema = new ArraySchema();
		schema.setItems(toSchema(mode, requestType, Optional.empty(), itemType.get(),
			requestType == RequestType.RESPONSE ? Optional.of(Boolean.FALSE) : Optional.empty()));
		nullable.ifPresent(schema::setNullable);
		return schema;
	    }
	}

	if (Map.class.isAssignableFrom(propertyType)) {
	    final @NonNull Optional<Class<?>> itemType = OptionalUtils.coalesce(
		    opPd.flatMap(PersistenceUtils::getElementCollectionTargetClass),
		    opPd.flatMap(pd -> ReflectionUtils.getCollectionGenericTypeArgument(pd, 1)));
	    final ObjectSchema schema = new ObjectSchema();
	    if (itemType.isPresent()) {
		schema.setAdditionalProperties(
			toSchema(mode, requestType, Optional.empty(), itemType.get(), Optional.empty()));
	    } else {
		schema.setAdditionalProperties(Boolean.TRUE);
	    }
	    nullable.ifPresent(schema::setNullable);
	    return schema;
	}

	if (ANY_VALUE_CLASS.equals(propertyType)) {
	    final ComposedSchema schema = new ComposedSchema();
	    schema.addOneOfItem(new BooleanSchema());
	    schema.addOneOfItem(new ObjectSchema().additionalProperties(Boolean.TRUE));
	    schema.addOneOfItem(new NumberSchema());
	    schema.addOneOfItem(new StringSchema());
	    schema.setNullable(true);
	    return schema;
	}

	final Optional<Supplier<Schema<?>>> standardSchemaSupplier = StandardSchemasProvider.getStandardSchemaSupplier(
		propertyType, taskProperties.isAddXJavaClassName(), taskProperties.isAddXJavaComparable());
	if (standardSchemaSupplier.isPresent()) {
	    final Schema<?> schema = standardSchemaSupplier.get().get();
	    nullable.ifPresent(schema::setNullable);
	    return schema;
	}

	final ClassMappingMode childPropertyMappingMode = isExposed.test(propertyType) ? ClassMappingMode.EXPOSED
		: ClassMappingMode.DATA_ITEM;
	final Schema<Object> refSchema = classToRefResolver.getRefSchema(propertyType, childPropertyMappingMode,
		requestType);
	return nullable.orElse(true) ? makeNullable(refSchema) : refSchema;
    }

}
