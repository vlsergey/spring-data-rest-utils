package io.github.vlsergey.springdatarestutils;

import org.junit.jupiter.api.Test;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.vlsergey.springdatarestutils.test.TestEntity;
import io.swagger.v3.oas.models.media.Schema;

class EntityToSchemaMapperTest {

    @Test
    void testMap() throws Exception {
	EntityToSchemaMapper mapper = new EntityToSchemaMapper(
		new RepositoryEnumerator(getClass().getPackageName() + ".test", RepositoryDetectionStrategies.ALL)
			.enumerate(getClass().getClassLoader()));

	final Schema<?> schema = mapper.map(TestEntity.class, ClassMappingMode.DATA_ITEM,
		(a, b) -> a.getSimpleName() + "Type");

	String json = JacksonHelper.writeValueAsString(false, schema);

	assertEquals("---\n" + //
		"properties:\n" + //
		"  created:\n" + //
		"    format: \"date-time\"\n" + //
		"    nullable: false\n" + //
		"    type: \"string\"\n" + //
		"  id:\n" + //
		"    format: \"uuid\"\n" + //
		"    type: \"string\"\n" + //
		"  updated:\n" + //
		"    format: \"date-time\"\n" + //
		"    nullable: false\n" + //
		"    type: \"string\"\n" + //
		"required:\n" + //
		"- \"created\"\n" + //
		"- \"updated\"\n" + //
		"type: \"object\"\n" + //
		"", json);
    }

}
