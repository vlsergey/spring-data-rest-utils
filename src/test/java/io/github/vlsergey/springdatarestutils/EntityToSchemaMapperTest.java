package io.github.vlsergey.springdatarestutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;

import org.junit.jupiter.api.Test;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.UriTemplate;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.vlsergey.springdatarestutils.CodebaseScannerFacade.ScanResult;
import io.github.vlsergey.springdatarestutils.projections.TestEntityDefaultProjection;
import io.github.vlsergey.springdatarestutils.test.TestEntity;
import io.swagger.v3.oas.models.media.Schema;
import lombok.Getter;

class EntityToSchemaMapperTest {

    private static final ScanResult emptyScanResult = new ScanResult(emptyMap(), emptySet(), emptySet(), emptySet());

    private final TaskProperties taskProperties = new TaskProperties().setAddXLinkedEntity(true);

    private final EntityToSchemaMapper mapper = new EntityToSchemaMapper(
	    (a, b, c) -> ClassToRefResolver.generateName(taskProperties, a, b, c), TestEntity.class::equals,
	    emptyScanResult, taskProperties);

    @Test
    void nullableArrayIsExposedAsNullable() {
	// it's required in response, because server will fill it (but may be null)
	assertEquals("required:\n" + //
		"- nullableArray\n" + //
		"type: object\n" + //
		"properties:\n" + //
		"  nullableArray:\n" + //
		"    type: array\n" + //
		"    nullable: true\n" + //
		"    items:\n" + //
		"      type: string\n" + //
		"", SchemaUtils.writeValueAsString(false, mapper.mapEntity(ClassWithNullableArray.class,
			ClassMappingMode.EXPOSED, RequestType.RESPONSE)));

	// but optional in create/update request
	assertEquals("type: object\n" + //
		"properties:\n" + //
		"  nullableArray:\n" + //
		"    type: array\n" + //
		"    nullable: true\n" + //
		"    items:\n" + //
		"      type: string\n" + //
		"", SchemaUtils.writeValueAsString(false, mapper.mapEntity(ClassWithNullableArray.class,
			ClassMappingMode.EXPOSED, RequestType.CREATE_OR_UPDATE)));
    }

    @Test
    void testLink() throws Exception {
	final Schema<?> schema = mapper.mapEntity(Link.class, ClassMappingMode.DATA_ITEM, RequestType.RESPONSE);
	String json = SchemaUtils.writeValueAsString(false, schema);

	assertEquals("type: object\n" + //
		"properties:\n" + //
		"  deprecation:\n" + //
		"    type: string\n" + //
		"    nullable: false\n" + //
		"  href:\n" + //
		"    type: string\n" + //
		"    nullable: false\n" + //
		"  hreflang:\n" + //
		"    type: string\n" + //
		"    nullable: false\n" + //
		"  media:\n" + //
		"    type: string\n" + //
		"    nullable: false\n" + //
		"  name:\n" + //
		"    type: string\n" + //
		"    nullable: false\n" + //
		"  profile:\n" + //
		"    type: string\n" + //
		"    nullable: false\n" + //
		"  rel:\n" + //
		"    type: string\n" + //
		"    nullable: false\n" + //
		"  templated:\n" + //
		"    type: boolean\n" + //
		"    nullable: false\n" + //
		"  title:\n" + //
		"    type: string\n" + //
		"    nullable: false\n" + //
		"  type:\n" + //
		"    type: string\n" + //
		"    nullable: false\n" + //
		"", json);
    }

    @Test
    void testMapAsDataItem() throws Exception {
	final Schema<?> schema = mapper.mapEntity(TestEntity.class, ClassMappingMode.DATA_ITEM, RequestType.RESPONSE);
	String json = SchemaUtils.writeValueAsString(false, schema);

	assertEquals("required:\n" + //
		"- created\n" + //
		"- id\n" + //
		"- parent\n" + //
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
		"    nullable: false\n" + //
		"  parent:\n" + //
		"    nullable: true\n" + //
		"    oneOf:\n" + //
		"    - $ref: '#/components/schemas/TestEntity'\n" + //
		"  updated:\n" + //
		"    type: string\n" + //
		"    format: date-time\n" + //
		"    nullable: false\n" + //
		"", json);
    }

    @Test
    void testMapAsExposed() throws Exception {
	final Schema<?> schema = mapper.mapEntity(TestEntity.class, ClassMappingMode.EXPOSED, RequestType.RESPONSE);
	String json = SchemaUtils.writeValueAsString(false, schema);

	assertEquals("required:\n" + //
		"- created\n" + //
		"- id\n" + //
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
		"    nullable: false\n" + //
		"  updated:\n" + //
		"    type: string\n" + //
		"    format: date-time\n" + //
		"    nullable: false\n" + //
		"", json);
    }

    @Test
    void testMapAsLinks() throws Exception {
	final Schema<?> schema = mapper.mapEntity(TestEntity.class, ClassMappingMode.LINKS, RequestType.RESPONSE);
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
    void testUriTemplate() throws Exception {
	final Schema<?> schema = mapper.mapEntity(UriTemplate.class, ClassMappingMode.DATA_ITEM, RequestType.RESPONSE);
	String json = SchemaUtils.writeValueAsString(false, schema);

	assertEquals("type: object\n" + //
		"", json);
    }

    @Test
    void testWithBeanPropertiesReturnsPropsFromAllSuperinterfaces() {
	List<String> props = new ArrayList<>();
	EntityToSchemaMapper.withBeanProperties(TestEntityDefaultProjection.class, pd -> props.add(pd.getName()));
	assertEquals(Arrays.asList("grandParent", "parent", "parentId", "id"), props);
    }

    @Getter
    public static class ClassWithNullableArray {
	@Column(nullable = true)
	private String[] nullableArray;
    }

}
