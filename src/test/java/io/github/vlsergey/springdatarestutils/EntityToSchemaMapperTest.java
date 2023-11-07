package io.github.vlsergey.springdatarestutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.Yaml;

class EntityToSchemaMapperTest {

    private static final ScanResult emptyScanResult = new ScanResult(emptyMap(), emptySet(), emptySet(), emptySet());

    private final TaskProperties taskProperties = new TaskProperties().setAddXLinkedEntity(true);

    private final Yaml yaml = new Yaml(new Constructor(Map.class));
    private final EntityToSchemaMapper mapper = new EntityToSchemaMapper(
	    (a, b, c) -> ClassToRefResolver.generateName(taskProperties, a, b, c),
	    new CustomAnnotationsHelper(taskProperties), TestEntity.class::equals,
	    new ProjectionHelper(emptyScanResult), emptyScanResult, taskProperties);

    @Test
    void testLink() throws Exception {
	final Schema<?> schema = mapper.mapEntity(Link.class, ClassMappingMode.DATA_ITEM, RequestType.RESPONSE);
	Map<String, Object> yamlMap = yaml.load(SchemaUtils.writeValueAsString(false, schema));

	assertEquals(yaml.load("type: object\n" + //
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
		""), yamlMap);
    }

    @Test
    void testMapAsDataItem() throws Exception {
	final Schema<?> schema = mapper.mapEntity(TestEntity.class, ClassMappingMode.DATA_ITEM, RequestType.RESPONSE);
	Map<String, Object> yamlMap = yaml.load(SchemaUtils.writeValueAsString(false, schema));

	assertEquals(yaml.load("required:\n" + //
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
		""), yamlMap);
    }

    @Test
    void testMapAsExposed() throws Exception {
	final Schema<?> schema = mapper.mapEntity(TestEntity.class, ClassMappingMode.EXPOSED, RequestType.RESPONSE);
	Map<String, Object> yamlMap = yaml.load(SchemaUtils.writeValueAsString(false, schema));

	assertEquals(yaml.load("required:\n" + //
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
		""), yamlMap);
    }

    @Test
    void testMapAsLinks() throws Exception {
	final Schema<?> schema = mapper.mapEntity(TestEntity.class, ClassMappingMode.LINKS, RequestType.RESPONSE);
	Map<String, Object> yamlMap = yaml.load(SchemaUtils.writeValueAsString(false, schema));

	assertEquals(yaml.load("required:\n" + //
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
		""), yamlMap);
    }

    @Test
    void testUriTemplate() throws Exception {
	final Schema<?> schema = mapper.mapEntity(UriTemplate.class, ClassMappingMode.DATA_ITEM, RequestType.RESPONSE);
	Map<String, Object> yamlMap = yaml.load(SchemaUtils.writeValueAsString(false, schema));

	assertEquals(yaml.load("required:\n" //
		+ "- variableNames\n" //
		+ "- variables\n" //
		+ "type: object\n" //
		+ "properties:\n" //
		+ "  variableNames:\n" //
		+ "    type: array\n" //
		+ "    nullable: false\n" //
		+ "    items:\n" //
		+ "      type: string\n" //
		+ "      nullable: false\n" //
		+ "  variables:\n" //
		+ "    type: array\n" //
		+ "    nullable: false\n" //
		+ "    items:\n" //
		+ "      $ref: '#/components/schemas/TemplateVariable'\n" //
		+ ""), yamlMap);
    }

    @Test
    void testWithBeanPropertiesReturnsPropsFromAllSuperinterfaces() {
	List<String> props = new ArrayList<>();
	EntityToSchemaMapper.withBeanProperties(TestEntityDefaultProjection.class, pd -> props.add(pd.getName()));
	assertEquals(Arrays.asList("grandParent", "parent", "parentId", "id"), props);
    }

}
