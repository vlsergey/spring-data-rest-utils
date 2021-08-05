package io.github.vlsergey.springdatarestutils;

import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import io.swagger.v3.oas.models.media.*;
import lombok.NonNull;

public class StandardSchemasProvider {

    private static final @NonNull Map<Class<?>, Supplier<Schema<?>>> standardSchemas = new LinkedHashMap<>();

    static {
	standardSchemas.put(Instant.class, DateTimeSchema::new);
	standardSchemas.put(Integer.class, IntegerSchema::new);

	standardSchemas.put(org.springframework.hateoas.LinkRelation.class, StringSchema::new);
	standardSchemas.put(Long.class, () -> new IntegerSchema().format("int64"));

	standardSchemas.put(Map.class, MapSchema::new);

	standardSchemas.put(String.class, StringSchema::new);

	standardSchemas.put(URI.class, UriSchema::new);
	standardSchemas.put(URL.class, UriSchema::new);
	standardSchemas.put(UUID.class, UUIDSchema::new);

	standardSchemas.put(boolean.class, () -> new BooleanSchema().nullable(Boolean.FALSE));
	standardSchemas.put(int.class, () -> new IntegerSchema().format("int32").nullable(Boolean.FALSE));
	standardSchemas.put(long.class, () -> new IntegerSchema().format("int64").nullable(Boolean.FALSE));
    }

    public static Optional<Supplier<Schema<?>>> getStandardSchemaSupplier(Class<?> cls, boolean withXJavaClassName,
	    boolean withXJavaComparable) {
	return standardSchemas.entrySet().stream().filter(e -> e.getKey().isAssignableFrom(cls)).map(Entry::getValue)
		.<Supplier<Schema<?>>>map(schemaProvider -> (withXJavaClassName || withXJavaComparable)
			&& (URL.class.isAssignableFrom(cls) || Comparable.class.isAssignableFrom(cls)) ? () -> {
			    Schema<?> result = schemaProvider.get();
			    if (withXJavaClassName) {
				result.addExtension(ExtensionConstants.X_JAVA_CLASS_NAME, cls.getName());
			    }
			    if (withXJavaComparable) {
				result.addExtension(ExtensionConstants.X_JAVA_COMPARABLE,
					Comparable.class.isAssignableFrom(cls));
			    }
			    return result;
			} : schemaProvider)
		.findFirst();
    }

    public static boolean hasStandardSchemaSupplier(Class<?> cls) {
	return standardSchemas.keySet().stream().anyMatch(e -> e.isAssignableFrom(cls));
    }

    public static final class UriSchema extends Schema<String> {
	public UriSchema() {
	    super("string", "uri");
	}
    }

}
