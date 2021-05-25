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

@SuppressWarnings({ "rawtypes" })
public class StandardSchemasProvider {

    private static final @NonNull Map<Class<?>, Supplier<Schema>> standardSchemas = new LinkedHashMap<>();

    static {
	standardSchemas.put(Instant.class, DateTimeSchema::new);
	standardSchemas.put(Integer.class, IntegerSchema::new);

	standardSchemas.put(org.springframework.hateoas.LinkRelation.class, StringSchema::new);
	standardSchemas.put(Long.class, NumberSchema::new);

	standardSchemas.put(Map.class, MapSchema::new);

	standardSchemas.put(String.class, StringSchema::new);

	standardSchemas.put(URI.class, UriSchema::new);
	standardSchemas.put(URL.class, UriSchema::new);
	standardSchemas.put(UUID.class, UUIDSchema::new);

	standardSchemas.put(boolean.class, () -> new BooleanSchema().nullable(Boolean.FALSE));
	standardSchemas.put(int.class, () -> new IntegerSchema().nullable(Boolean.FALSE));
	standardSchemas.put(long.class, () -> new NumberSchema().nullable(Boolean.FALSE));
    }

    public static Optional<Supplier<Schema>> getStandardSchemaSupplier(Class<?> cls) {
	return standardSchemas.entrySet().stream().filter(e -> e.getKey().isAssignableFrom(cls)).map(Entry::getValue)
		.findFirst();
    }

    public static final class UriSchema extends Schema<String> {
	public UriSchema() {
	    super("string", "uri");
	}
    }

}
