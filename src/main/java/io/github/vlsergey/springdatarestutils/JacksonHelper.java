package io.github.vlsergey.springdatarestutils;

import static java.util.Collections.singleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter.SerializeExceptFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import lombok.NonNull;
import lombok.SneakyThrows;

public class JacksonHelper {

    // it's not strict, but nice to have fields in this particular order
    // https://swagger.io/specification/#schema
    private static final String[] OPENAPI_FIELDS_SPEC_ORDER = new String[] { "openapi", "info", "servers", "paths",
	    "components", "security", "tags", "externalDocs" };

    private static final String SCHEMA_FILTER_ID = "SchemaFilter";

    private static @NonNull JsonSerializer<?> improveOpenApiSerializer(final @NonNull BeanDescription desc,
	    final @NonNull BeanSerializer oldSerializer) {
	Map<String, BeanPropertyWriter> oldWriters = new LinkedHashMap<>();
	oldSerializer.properties().forEachRemaining(w -> oldWriters.put(w.getName(), (BeanPropertyWriter) w));

	// https://swagger.io/specification/#schema
	final List<BeanPropertyWriter> newWriters = Arrays.stream(OPENAPI_FIELDS_SPEC_ORDER).map(oldWriters::remove)
		.filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));

	// add other items if present
	oldWriters.values().forEach(newWriters::add);

	final BeanPropertyWriter[] array = newWriters.toArray(BeanPropertyWriter[]::new);

	// just because constructor is invisible
	return new BeanSerializer(oldSerializer, array, array) {
	    private static final long serialVersionUID = 1L;
	};
    }

    @SneakyThrows
    public static String writeValueAsString(boolean json, Object obj) {

	final DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("  ", "\n");

	final DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
	printer.indentObjectsWith(indenter);
	printer.indentArraysWith(indenter);

	final ObjectMapper jsonMapper = json ? new ObjectMapper() : new ObjectMapper(new YAMLFactory());
	jsonMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
	jsonMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

	jsonMapper.registerModule(new SimpleModule() {
	    private static final long serialVersionUID = 1L;

	    @Override
	    public void setupModule(SetupContext context) {
		super.setupModule(context);
		context.addBeanSerializerModifier(new BeanSerializerModifier() {
		    @Override
		    public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription desc,
			    JsonSerializer<?> serializer) {
			if (Schema.class.isAssignableFrom(desc.getBeanClass())) {
			    return ((BeanSerializer) serializer).withFilterId(SCHEMA_FILTER_ID);
			}
			if (OpenAPI.class.isAssignableFrom(desc.getBeanClass())
				&& serializer instanceof BeanSerializer) {
			    return improveOpenApiSerializer(desc, (BeanSerializer) serializer);
			}
			return serializer;
		    }

		});
	    }
	});

	final SimpleFilterProvider filterProvider = new SimpleFilterProvider();
	filterProvider.addFilter(SCHEMA_FILTER_ID, new SerializeExceptFilter(singleton("exampleSetFlag")) {
	    private static final long serialVersionUID = 1L;

	    @Override
	    @SneakyThrows
	    public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider,
		    PropertyWriter writer) throws Exception {
		if (writer.getName().equals("extensions")) {
		    Schema<?> schema = (Schema<?>) pojo;
		    if (schema.getExtensions() != null) {
			for (Map.Entry<String, Object> entry : schema.getExtensions().entrySet()) {
			    jgen.writeObjectField(entry.getKey(), entry.getValue());
			}
		    }
		} else {
		    super.serializeAsField(pojo, jgen, provider, writer);
		}
	    }
	});

	jsonMapper.setFilterProvider(filterProvider);
	jsonMapper.setSerializationInclusion(Include.NON_ABSENT);
	return jsonMapper.writer(printer).writeValueAsString(obj);
    }

}
