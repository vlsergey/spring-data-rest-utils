package io.github.vlsergey.springdatarestutils;

import java.beans.BeanInfo;
import java.beans.FeatureDescriptor;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiFunction;
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

    private static final Field shortDescriptionField;

    static {
	shortDescriptionField = getDeclaredField(FeatureDescriptor.class, "shortDescription");
    }

    private final @NonNull Predicate<@NonNull Class<?>> isExposed;

    private final @NonNull ScanResult scanResult;

    private final @NonNull TaskProperties taskProperties;

    private static String buildRef(BiFunction<Class<?>, ClassMappingMode, String> getReferencedTypeName, Class<?> cls,
	    ClassMappingMode mode) {
	return "#/components/schemas/" + getReferencedTypeName.apply(cls, mode);
    }

    static Schema<?> buildRefSchema(BiFunction<Class<?>, ClassMappingMode, String> getReferencedTypeName, Class<?> cls,
	    ClassMappingMode mode) {
	return new Schema<>().$ref(buildRef(getReferencedTypeName, cls, mode));
    }

    @SuppressWarnings("rawtypes")
    static Schema<?> buildRootCollectionSchema(String linkTypeName, String collectionKey, Schema itemRefSchema) {
	ObjectSchema result = new ObjectSchema();
	result.setRequired(Arrays.asList("_embedded", "_links", "page"));

	result.addProperties("_embedded", new ObjectSchema().addRequiredItem(collectionKey).addProperties(collectionKey,
		new ArraySchema().items(itemRefSchema)));

	final Schema<?> linkSchema = new Schema<>().$ref("#/components/schemas/" + linkTypeName);

	result.addProperties("_links",
		new ObjectSchema().addRequiredItem("self").addRequiredItem("profile").addRequiredItem("search")
			.addProperties("self", linkSchema).addProperties("profile", linkSchema)
			.addProperties("search", linkSchema));

	Schema int32NonNegativeSchema = new IntegerSchema().format("int32").minimum(BigDecimal.ZERO)
		.nullable(Boolean.FALSE);
	Schema int64NonNegativeSchema = new IntegerSchema().format("int64").minimum(BigDecimal.ZERO)
		.nullable(Boolean.FALSE);

	result.addProperties("page", new ObjectSchema().addRequiredItem("size").addRequiredItem("totalElements")
		.addRequiredItem("totalPages").addRequiredItem("number").addProperties("size", int32NonNegativeSchema)
		.addProperties("totalElements", int64NonNegativeSchema)
		.addProperties("totalPages", int32NonNegativeSchema).addProperties("number", int32NonNegativeSchema));

	return result;
    }

    @SneakyThrows
    private static Field getDeclaredField(Class<?> cls, String fieldName) {
	final Field field = cls.getDeclaredField(fieldName);
	field.setAccessible(true);
	return field;
    }

    @SneakyThrows
    private static boolean hasShortDescription(FeatureDescriptor pd) {
	return shortDescriptionField.get(pd) != null;
    }

    @SneakyThrows
    private static Stream<PropertyDescriptor> withBeanProperties(Class<?> cls) {
	final BeanInfo beanInfo = Introspector.getBeanInfo(cls);
	return Arrays.stream(beanInfo.getPropertyDescriptors())
		.filter(pd -> !pd.getReadMethod().getDeclaringClass().getName().startsWith("java.lang."));
    }

    private static void withBeanProperties(Class<?> cls, Consumer<PropertyDescriptor> consumer) {
	withBeanProperties(cls).forEach(consumer);
    }

    @SneakyThrows
    private Schema<?> buildEntityLinksSchema(Class<?> cls, ClassMappingMode mode,
	    BiFunction<Class<?>, ClassMappingMode, String> getReferencedTypeName) {
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
	    final Schema<?> refSchema = buildRefSchema(getReferencedTypeName, Link.class, ClassMappingMode.DATA_ITEM);
	    if (!taskProperties.isAddXLinkedEntity()) {
		linksSchema.getProperties().put(key, refSchema);
		return;
	    }

	    ComposedSchema composedSchema = new ComposedSchema();
	    composedSchema.addAllOfItem(refSchema);
	    composedSchema.addExtension(ExtensionConstants.X_LINKED_ENTITY,
		    getReferencedTypeName.apply(linkClass, ClassMappingMode.EXPOSED));
	    linksSchema.getProperties().put(key, composedSchema);
	});

	final ObjectSchema objectSchema = new ObjectSchema();
	objectSchema.setName(getReferencedTypeName.apply(cls, mode));
	objectSchema.addRequiredItem("_links");
	initPropertiesIfNotYet(objectSchema).put("_links", linksSchema);
	return objectSchema;
    }

    private @NonNull ComposedSchema buildHierarchyRootSchema(
	    final @NonNull BiFunction<Class<?>, ClassMappingMode, String> getReferencedTypeName,
	    final @NonNull Class<?> cls) {
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
			    buildRef(getReferencedTypeName, childClass, childClassMode));
		}
	    });

	    composedSchema.addAnyOfItem(buildRefSchema(getReferencedTypeName, childClass, childClassMode));
	});

	return composedSchema;
    }

    private ObjectSchema buildObjectSchema(Class<?> cls, ClassMappingMode mode,
	    BiFunction<Class<?>, ClassMappingMode, String> getReferencedTypeName) {
	final ObjectSchema objectSchema = new ObjectSchema();
	objectSchema.setName(getReferencedTypeName.apply(cls, mode));

	withBeanProperties(cls, pd -> {
	    final Class<?> propertyType = pd.getPropertyType();
	    final Boolean nullable = AnnotationHelper.getNullable(cls, pd);

	    if (mode == ClassMappingMode.INHERITANCE_CHILD) {
		if (!pd.getReadMethod().getDeclaringClass().equals(cls)) {
		    return;
		}
	    }

	    boolean isMappedEntityType = isExposed.test(propertyType);
	    if (isMappedEntityType && !mode.isMappedEntitiesExpoded()) {
		return;
	    }

	    if (mode != ClassMappingMode.EXPOSED_PATCH && nullable != null && !nullable) {
		objectSchema.addRequiredItem(pd.getName());
	    }

	    if (propertyType.isEnum()) {
		initPropertiesIfNotYet(objectSchema).put(pd.getName(),
			buildRefSchema(getReferencedTypeName, pd.getPropertyType(), ClassMappingMode.DATA_ITEM));
		return;
	    }

	    final Optional<Supplier<Schema>> standardSchemaSupplier = StandardSchemasProvider
		    .getStandardSchemaSupplier(propertyType, taskProperties.isAddXSortable());
	    if (standardSchemaSupplier.isPresent()) {
		final Schema<?> schema = standardSchemaSupplier.get().get();
		if (mode != ClassMappingMode.EXPOSED_PATCH && schema.getNullable() != null && !schema.getNullable()
			&& (objectSchema.getRequired() == null || !objectSchema.getRequired().contains(pd.getName()))) {
		    objectSchema.addRequiredItem(pd.getName());
		}
		if (null != nullable) {
		    schema.setNullable(nullable);
		}
		initPropertiesIfNotYet(objectSchema).put(pd.getName(), schema);
		return;
	    }

	    if (Collection.class.isAssignableFrom(propertyType)) {
		// not yet implemented
		return;
	    }

	    initPropertiesIfNotYet(objectSchema).put(pd.getName(), buildRefSchema(getReferencedTypeName, propertyType,
		    isMappedEntityType ? ClassMappingMode.EXPOSED : ClassMappingMode.DATA_ITEM));
	});
	return objectSchema;
    }

    @SneakyThrows
    private Schema<?> buildWithLinksSchema(Class<?> cls) {
	Schema<?> withoutLinks = new Schema<>()
		.$ref("#/components/schemas/" + ClassMappingMode.EXPOSED.getName(taskProperties, cls));
	Schema<?> links = new Schema<>()
		.$ref("#/components/schemas/" + ClassMappingMode.LINKS.getName(taskProperties, cls));

	return new ComposedSchema().addAllOfItem(withoutLinks).addAllOfItem(links);
    }

    @SuppressWarnings("rawtypes")
    private Map<String, Schema> initPropertiesIfNotYet(final ObjectSchema objectSchema) {
	if (objectSchema.getProperties() == null) {
	    objectSchema.setProperties(new TreeMap<>());
	}
	return objectSchema.getProperties();
    }

    @SneakyThrows
    public Schema<?> mapEntity(Class<?> cls, ClassMappingMode mode,
	    BiFunction<Class<?>, ClassMappingMode, String> getReferencedTypeName) {
	if (mode == ClassMappingMode.LINKS) {
	    return buildEntityLinksSchema(cls, mode, getReferencedTypeName);
	}

	if (mode == ClassMappingMode.WITH_LINKS) {
	    return buildWithLinksSchema(cls);
	}

	if (mode == ClassMappingMode.INHERITANCE_CHILD) {
	    ComposedSchema composedSchema = new ComposedSchema();
	    final Class<?> baseClass = scanResult.getInheritance().entrySet().stream()
		    .filter(e -> e.getValue().contains(cls)).findAny().get().getKey();
	    composedSchema
		    .addAllOfItem(buildRefSchema(getReferencedTypeName, baseClass, ClassMappingMode.INHERITANCE_BASE));
	    composedSchema.addAllOfItem(buildObjectSchema(cls, mode, getReferencedTypeName));
	    return composedSchema;
	}

	if (cls.isEnum()) {
	    return mapEnum((Class) cls);
	}

	final Optional<Supplier<Schema>> entityStandardSchemaSupplier = StandardSchemasProvider
		.getStandardSchemaSupplier(cls, taskProperties.isAddXSortable());
	if (entityStandardSchemaSupplier.isPresent()) {
	    return entityStandardSchemaSupplier.get().get();
	}

	if (mode == ClassMappingMode.EXPOSED && this.scanResult.getInheritance().containsKey(cls)) {
	    return buildHierarchyRootSchema(getReferencedTypeName, cls);
	}

	return buildObjectSchema(cls, mode, getReferencedTypeName);
    }

    private <T extends Enum<T>> StringSchema mapEnum(Class<T> cls) {
	final StringSchema stringSchema = new StringSchema();
	for (T constant : cls.getEnumConstants()) {
	    stringSchema.addEnumItem(constant.name());
	}
	return stringSchema;
    }

}
