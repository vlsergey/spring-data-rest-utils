package io.github.vlsergey.springdatarestutils;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter.SerializeExceptFilter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import static java.util.Collections.singleton;

import io.swagger.v3.oas.models.media.Schema;
import lombok.SneakyThrows;

public class JacksonHelper {

    private static final String SCHEMA_FILTER_ID = "SchemaFilter";

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
