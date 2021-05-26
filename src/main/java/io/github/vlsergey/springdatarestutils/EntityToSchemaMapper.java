package io.github.vlsergey.springdatarestutils;

import java.beans.BeanInfo;
import java.beans.FeatureDescriptor;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.hateoas.Link;
import org.springframework.util.StringUtils;

import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

@AllArgsConstructor
public class EntityToSchemaMapper {

    private static final Field shortDescriptionField;

    static {
	shortDescriptionField = getDeclaredField(FeatureDescriptor.class, "shortDescription");
    }

    private final @NonNull Set<RepositoryMetadata> interfaces;

    static Schema<?> buildRefSchema(BiFunction<Class<?>, ClassMappingMode, String> getReferencedTypeName, Class<?> cls,
	    ClassMappingMode mode) {
	Schema<?> schema = new Schema<>();
	schema.set$ref("#/components/schemas/" + getReferencedTypeName.apply(cls, mode));
	return schema;
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

    @SuppressWarnings("rawtypes")
    private Map<String, Schema> initPropertiesIfNotYet(final ObjectSchema objectSchema) {
	if (objectSchema.getProperties() == null) {
	    objectSchema.setProperties(new TreeMap<>());
	}
	return objectSchema.getProperties();
    }

    private boolean isExported(Class<?> cls) {
	return interfaces.stream().anyMatch(meta -> meta.getDomainType().isAssignableFrom(cls));
    }

    @SneakyThrows
    public Schema<?> map(Class<?> cls, ClassMappingMode mode, boolean addXLinkedEntity,
	    BiFunction<Class<?>, ClassMappingMode, String> getReferencedTypeName) {
	if (cls.isEnum() && mode != ClassMappingMode.ENUM || !cls.isEnum() && mode == ClassMappingMode.ENUM) {
	    throw new AssertionError("Incorrect mode " + mode + " for class " + cls.getName());
	}

	if (cls.isEnum()) {
	    return mapEnum((Class) cls);
	}

	if (mode == ClassMappingMode.TOP_LEVEL_ENTITY) {
	    final Map<String, Class<?>> links = new TreeMap<>();
	    links.put("self", cls);
	    links.put(StringUtils.uncapitalize(cls.getSimpleName()), cls);

	    final BeanInfo beanInfo = Introspector.getBeanInfo(cls);
	    for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
		final Class<?> propertyType = pd.getPropertyType();
		if (Collection.class.isAssignableFrom(propertyType)) {
		    // not yet implemented
		    continue;
		}
		if (!isExported(propertyType)) {
		    continue;
		}
		links.put(pd.getName(), propertyType);
	    }

	    ObjectSchema linksSchema = new ObjectSchema();
	    linksSchema.setProperties(new TreeMap<>());
	    links.forEach((key, linkClass) -> {
		final Schema<?> refSchema = buildRefSchema(getReferencedTypeName, Link.class,
			ClassMappingMode.DATA_ITEM);
		if (!addXLinkedEntity) {
		    linksSchema.getProperties().put(key, refSchema);
		    return;
		}

		ComposedSchema composedSchema = new ComposedSchema();
		composedSchema.addAllOfItem(refSchema);
		composedSchema.addExtension("x-linked-entity",
			getReferencedTypeName.apply(linkClass, ClassMappingMode.TOP_LEVEL_ENTITY));
		linksSchema.getProperties().put(key, composedSchema);
	    });

	    final ObjectSchema objectSchema = new ObjectSchema();
	    objectSchema.setName(getReferencedTypeName.apply(cls, mode));
	    objectSchema.addRequiredItem("_links");
	    initPropertiesIfNotYet(objectSchema).put("_links", linksSchema);

	    final ComposedSchema resultSchema = new ComposedSchema();
	    resultSchema.addAllOfItem(buildRefSchema(getReferencedTypeName, cls, ClassMappingMode.SECOND_LEVEL));
	    resultSchema.addAllOfItem(objectSchema);
	    return resultSchema;
	}

	final BeanInfo beanInfo = Introspector.getBeanInfo(cls);

	final ObjectSchema objectSchema = new ObjectSchema();
	objectSchema.setName(getReferencedTypeName.apply(cls, mode));

	for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
	    if (pd.getReadMethod().getDeclaringClass().getName().startsWith("java.lang.")) {
		continue;
	    }
	    final Class<?> propertyType = pd.getPropertyType();
	    final Boolean nullable = AnnotationHelper.getNullable(cls, pd);

	    boolean isMappedEntityType = isExported(propertyType);
	    if (isMappedEntityType && !mode.isMappedEntitiesExpoded()) {
		continue;
	    }

	    if (nullable != null && !nullable) {
		objectSchema.addRequiredItem(pd.getName());
	    }

	    if (propertyType.isEnum()) {
		initPropertiesIfNotYet(objectSchema).put(pd.getName(),
			buildRefSchema(getReferencedTypeName, pd.getPropertyType(), ClassMappingMode.ENUM));
		continue;
	    }

	    final Optional<Supplier<Schema>> standardSchemaSupplier = StandardSchemasProvider
		    .getStandardSchemaSupplier(propertyType);
	    if (standardSchemaSupplier.isPresent()) {
		final Schema<?> schema = standardSchemaSupplier.get().get();
		if (schema.getNullable() != null && !schema.getNullable()
			&& (objectSchema.getRequired() == null || !objectSchema.getRequired().contains(pd.getName()))) {
		    objectSchema.addRequiredItem(pd.getName());
		}
		if (null != nullable) {
		    schema.setNullable(nullable);
		}
		initPropertiesIfNotYet(objectSchema).put(pd.getName(), schema);
		continue;
	    }

	    if (Collection.class.isAssignableFrom(propertyType)) {
		// not yet implemented
		continue;
	    }

	    initPropertiesIfNotYet(objectSchema).put(pd.getName(), buildRefSchema(getReferencedTypeName, propertyType,
		    isMappedEntityType ? ClassMappingMode.SECOND_LEVEL : ClassMappingMode.DATA_ITEM));
	}

	return objectSchema;
    }

    private <T extends Enum<T>> StringSchema mapEnum(Class<T> cls) {
	final StringSchema stringSchema = new StringSchema();
	for (T constant : cls.getEnumConstants()) {
	    stringSchema.addEnumItem(constant.name());
	}
	return stringSchema;
    }

}
