package io.github.vlsergey.springdatarestutils;

import java.util.Map;
import java.util.TreeMap;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import lombok.SneakyThrows;

class SchemaUtils {

    private static final String APPLICATION_JSON_VALUE = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

    static <K extends Comparable<K>, V> void sortMapByKeys(Map<K, V> map) {
	Map<K, V> sorted = new TreeMap<>(map);
	map.clear();
	map.putAll(sorted);
    }

    static Content toContent(Schema<?> schema) {
	final MediaType mediaType = new MediaType().schema(schema);
	return new Content().addMediaType(APPLICATION_JSON_VALUE, mediaType);
    }

    @SneakyThrows
    static String writeValueAsString(boolean json, Object obj) {
	if (json) {
	    return Json.pretty().writeValueAsString(obj);
	} else {
	    return Yaml.pretty().writeValueAsString(obj);
	}
    }

}
