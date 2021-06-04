package io.github.vlsergey.springdatarestutils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.vlsergey.springdatarestutils.test.TestEntity;
import io.swagger.v3.oas.models.media.Schema;

class EntityToSchemaMapperTest {

    @Test
    void testMapAsDataItem() throws Exception {
	final TaskProperties taskProperties = new TaskProperties();
	final EntityToSchemaMapper mapper = new EntityToSchemaMapper(TestEntity.class::equals, taskProperties);

	final Schema<?> schema = mapper.mapEntity(TestEntity.class, ClassMappingMode.DATA_ITEM,
		(a, b) -> b.getName(taskProperties, a));

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
		"    $ref: \"#/components/schemas/TestEntity\"\n" + //
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
    void testMapAsExposed() throws Exception {
	final TaskProperties taskProperties = new TaskProperties().setAddXSortable(true);
	final EntityToSchemaMapper mapper = new EntityToSchemaMapper(TestEntity.class::equals, taskProperties);

	final Schema<?> schema = mapper.mapEntity(TestEntity.class, ClassMappingMode.EXPOSED,
		(a, b) -> b.getName(taskProperties, a));

	String json = JacksonHelper.writeValueAsString(false, schema);

	assertEquals("---\n" + //
		"properties:\n" + //
		"  created:\n" + //
		"    x-sortable: true\n" + //
		"    format: \"date-time\"\n" + //
		"    nullable: false\n" + //
		"    type: \"string\"\n" + //
		"  id:\n" + //
		"    x-sortable: true\n" + //
		"    format: \"uuid\"\n" + //
		"    type: \"string\"\n" + //
		"  updated:\n" + //
		"    x-sortable: true\n" + //
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
    void testMapAsLinks() throws Exception {
	final TaskProperties taskProperties = new TaskProperties().setAddXLinkedEntity(true);
	final EntityToSchemaMapper mapper = new EntityToSchemaMapper(TestEntity.class::equals, taskProperties);

	final Schema<?> schema = mapper.mapEntity(TestEntity.class, ClassMappingMode.LINKS,
		(a, b) -> b.getName(taskProperties, a));

	String json = JacksonHelper.writeValueAsString(false, schema);

	assertEquals("---\n" + //
		"properties:\n" + //
		"  _links:\n" + //
		"    properties:\n" + //
		"      parent:\n" + //
		"        allOf:\n" + //
		"        - $ref: \"#/components/schemas/Link\"\n" + //
		"        x-linked-entity: \"TestEntity\"\n" + //
		"      self:\n" + //
		"        allOf:\n" + //
		"        - $ref: \"#/components/schemas/Link\"\n" + //
		"        x-linked-entity: \"TestEntity\"\n" + //
		"      testEntity:\n" + //
		"        allOf:\n" + //
		"        - $ref: \"#/components/schemas/Link\"\n" + //
		"        x-linked-entity: \"TestEntity\"\n" + //
		"    type: \"object\"\n" + //
		"required:\n" + //
		"- \"_links\"\n" + //
		"type: \"object\"\n" + //
		"", json);
    }

}
