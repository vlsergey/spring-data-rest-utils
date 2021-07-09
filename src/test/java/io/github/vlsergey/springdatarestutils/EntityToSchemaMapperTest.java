package io.github.vlsergey.springdatarestutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.vlsergey.springdatarestutils.CodebaseScannerFacade.ScanResult;
import io.github.vlsergey.springdatarestutils.projections.TestEntityDefaultProjection;
import io.github.vlsergey.springdatarestutils.test.TestEntity;
import io.swagger.v3.oas.models.media.Schema;

class EntityToSchemaMapperTest {

    private static final ScanResult emptyScanResult = new ScanResult(emptyMap(), emptySet(), emptySet(), emptySet());

    @Test
    void testMapAsDataItem() throws Exception {
	final TaskProperties taskProperties = new TaskProperties();
	final EntityToSchemaMapper mapper = new EntityToSchemaMapper((a, b) -> b.getName(taskProperties, a),
		TestEntity.class::equals, emptyScanResult, taskProperties);

	final Schema<?> schema = mapper.mapEntity(TestEntity.class, ClassMappingMode.DATA_ITEM);

	String json = SchemaUtils.writeValueAsString(false, schema);

	assertEquals("required:\n" + //
		"- created\n" + //
		"- updated\n" + //
		"type: object\n" + //
		"properties:\n" + //
		"  created:\n" + //
		"    type: string\n" + //
		"    format: date-time\n" + //
		"    nullable: false\n" + //
		"  id:\n" + //
		"    type: string\n" + //
		"    format: uuid\n" + //
		"  parent:\n" + //
		"    $ref: '#/components/schemas/TestEntityRequest'\n" + //
		"  updated:\n" + //
		"    type: string\n" + //
		"    format: date-time\n" + //
		"    nullable: false\n" + //
		"", json);
    }

    @Test
    void testMapAsExposed() throws Exception {
	final TaskProperties taskProperties = new TaskProperties().setAddXSortable(true);
	final EntityToSchemaMapper mapper = new EntityToSchemaMapper((a, b) -> b.getName(taskProperties, a),
		TestEntity.class::equals, emptyScanResult, taskProperties);

	final Schema<?> schema = mapper.mapEntity(TestEntity.class, ClassMappingMode.EXPOSED_SUBMIT);

	String json = SchemaUtils.writeValueAsString(false, schema);

	assertEquals("required:\n" + //
		"- created\n" + //
		"- updated\n" + //
		"type: object\n" + //
		"properties:\n" + //
		"  created:\n" + //
		"    type: string\n" + //
		"    format: date-time\n" + //
		"    nullable: false\n" + //
		"    x-sortable: true\n" + //
		"  id:\n" + //
		"    type: string\n" + //
		"    format: uuid\n" + //
		"    x-sortable: true\n" + //
		"  updated:\n" + //
		"    type: string\n" + //
		"    format: date-time\n" + //
		"    nullable: false\n" + //
		"    x-sortable: true\n" + //
		"", json);
    }

    @Test
    void testMapAsLinks() throws Exception {
	final TaskProperties taskProperties = new TaskProperties().setAddXLinkedEntity(true);
	final EntityToSchemaMapper mapper = new EntityToSchemaMapper((a, b) -> b.getName(taskProperties, a),
		TestEntity.class::equals, emptyScanResult, taskProperties);

	final Schema<?> schema = mapper.mapEntity(TestEntity.class, ClassMappingMode.LINKS);

	String json = SchemaUtils.writeValueAsString(false, schema);

	assertEquals("required:\n" + //
		"- _links\n" + //
		"type: object\n" + //
		"properties:\n" + //
		"  _links:\n" + //
		"    type: object\n" + //
		"    properties:\n" + //
		"      parent:\n" + //
		"        allOf:\n" + //
		"        - $ref: '#/components/schemas/Link'\n" + //
		"        x-linked-entity: TestEntity\n" + //
		"      self:\n" + //
		"        allOf:\n" + //
		"        - $ref: '#/components/schemas/Link'\n" + //
		"        x-linked-entity: TestEntity\n" + //
		"      testEntity:\n" + //
		"        allOf:\n" + //
		"        - $ref: '#/components/schemas/Link'\n" + //
		"        x-linked-entity: TestEntity\n" + //
		"", json);
    }

    @Test
    void testWithBeanPropertiesReturnsPropsFromAllSuperinterfaces() {
	List<String> props = new ArrayList<>();
	EntityToSchemaMapper.withBeanProperties(TestEntityDefaultProjection.class, pd -> props.add(pd.getName()));
	assertEquals(Arrays.asList("grandParent", "parent", "parentId", "id"), props);
    }

}
