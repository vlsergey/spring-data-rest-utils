package io.github.vlsergey.springdatarestutils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.vlsergey.springdatarestutils.test.TestEntity;
import io.swagger.v3.oas.models.media.Schema;

class EntityToSchemaMapperTest {

    @Test
    void testMapAsDataItem() throws Exception {
	final EntityToSchemaMapper mapper = new EntityToSchemaMapper(TestEntity.class::equals);

	final Schema<?> schema = mapper.map(TestEntity.class, ClassMappingMode.DATA_ITEM, false, false,
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
		"  parent:\n" + //
		"    $ref: \"#/components/schemas/TestEntityType\"\n" + //
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

    @Test
    void testMapAsTopLevelEntity() throws Exception {
	final EntityToSchemaMapper mapper = new EntityToSchemaMapper(TestEntity.class::equals);

	final Schema<?> schema = mapper.map(TestEntity.class, ClassMappingMode.EXPOSED_WITH_LINKS, true, false,
		(a, b) -> a.getSimpleName() + "Type");

	String json = JacksonHelper.writeValueAsString(false, schema);

	assertEquals("---\n" + //
		"allOf:\n" + //
		"- $ref: \"#/components/schemas/TestEntityType\"\n" + //
		"- properties:\n" + //
		"    _links:\n" + //
		"      properties:\n" + //
		"        parent:\n" + //
		"          allOf:\n" + //
		"          - $ref: \"#/components/schemas/LinkType\"\n" + //
		"          x-linked-entity: \"TestEntityType\"\n" + //
		"        self:\n" + //
		"          allOf:\n" + //
		"          - $ref: \"#/components/schemas/LinkType\"\n" + //
		"          x-linked-entity: \"TestEntityType\"\n" + //
		"        testEntity:\n" + //
		"          allOf:\n" + //
		"          - $ref: \"#/components/schemas/LinkType\"\n" + //
		"          x-linked-entity: \"TestEntityType\"\n" + //
		"      type: \"object\"\n" + //
		"  required:\n" + //
		"  - \"_links\"\n" + //
		"  type: \"object\"\n" + //
		"", json);
    }

}
