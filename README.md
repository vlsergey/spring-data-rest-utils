# Gradle plugin for Spring Data JPA REST and OpenAPI  

[![Build with Gradle](https://github.com/vlsergey/spring-data-rest-utils/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/vlsergey/spring-data-rest-utils/actions/workflows/build.yml)
[![Gradle Plugin](https://img.shields.io/maven-metadata/v?label=Gradle%20Plugin&metadataUrl=https://plugins.gradle.org/m2/io/github/vlsergey/spring-data-rest-utils/io.github.vlsergey.spring-data-rest-utils.gradle.plugin/maven-metadata.xml)](https://plugins.gradle.org/plugin/io.github.vlsergey.spring-data-rest-utils)

Generates [OpenAPI specification](https://swagger.io/specification/) from JPA repositories exposed via [Spring Data REST](https://spring.io/projects/spring-data-rest) library.

- [x] Generates schema components for both exposed and non-exposed classes
- [x] Supports simple cases of inheritance with discriminators
- [x] Exposes open projections and default projections
- [X] Generates path and operations for supported commands
  - Generates path items for CRUD methods: `findAll`, `findOneById`, `deleteById`, `save` (incl. PUT, POST and PATCH)
  - Generates path items to fetch linked entities, create or delete assotiations
  - (WIP) Generates path items for query methods (so far that accepts and returns "simple" types like `Long` or `String` and single entity results like `Entity` or `Optional<Entity>`)

Examples:
- [Example of generated specification](https://github.com/vlsergey/spring-data-rest-utils/blob/master/src/test/resources/io/github/vlsergey/springdatarestutils/expected-example.yaml). [View in Swagger Editor Online](https://editor.swagger.io/?url=https://raw.githubusercontent.com/vlsergey/spring-data-rest-utils/master/src/test/resources/io/github/vlsergey/springdatarestutils/expected-example.yaml).
- [Example package used for generation](https://github.com/vlsergey/spring-data-rest-utils/tree/master/src/test/java/io/github/vlsergey/springdatarestutils/example)

## Enabling Gradle plugin
Using the plugins DSL:
```groovy
plugins {
  id "io.github.vlsergey.spring-data-rest-utils" version "0.40.0"
}
```

Using [legacy plugin application](https://docs.gradle.org/current/userguide/plugins.html#sec:old_plugin_application):
```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "io.github.vlsergey.springdatarestutils:spring-data-rest-utils:0.40.0"
  }
}

apply plugin: "io.github.vlsergey.spring-data-rest-utils"
```

Currently following tasks are supported:
- `generateOpenAPIForSpringDataREST` -- generates [OpenAPI specification](https://swagger.io/specification/) from JPA repositories

## Running
`gradle generateOpenAPIForSpringDataREST`

## Properties

Include the following snippet into `build.gradle`
```groovy
springdatarestutils {
    addXCustomAnnotations = ['org.tempuri.data.MyAnnotation'];
    addXJavaClassName = true;
    addXJavaComparable = true;
    addXLinkedEntity = true;
    basePackage = 'org.myname.myapp';
    info.get().with {
        title = 'MyApplication Data API';
    }
    servers.set([new io.swagger.v3.oas.models.servers.Server().url('/api/data')]);
    output = file('../frontend/src/MyAppApi.yaml');
    repositoryDetectionStrategy = 'ALL'
}
```

### Main properties

* `basePackage`<br>
type: `String`<br>
default: `""`

Base package to search JPA repository interfaces in.

* `output`<br>
type: `File`<br>
default: `"api.yaml"`

Where to output result. Supports both `.json` and `.yaml` extensions.

* `repositoryDetectionStrategy`<br>
type: `String`<br>
default: `"DEFAULT"`

The strategy to determine whether a given repository is to be exported by Spring Data REST. Values (and actual implementation) are reused from [Spring Data REST `RepositoryDetectionStrategies`](https://docs.spring.io/spring-data/rest/docs/current/api/org/springframework/data/rest/core/mapping/RepositoryDetectionStrategy.RepositoryDetectionStrategies.html)

### Specification Customization 

* `addXCustomAnnotations`<br>
type: `List<String>`<br>
default: empty list

Include additional `x-annotation-simple-name` [extension](https://swagger.io/docs/specification/openapi-extensions/) to property when field (read method) is annotated with annotation of such classes. Can be used for custom validation or UI improvements like provising `@Multiline` to generate `textarea` instead of `input`.

* `addXJavaClassName`<br>
type: `boolean`<br>
default: `false`

Include additional string `x-java-class-name` [extension](https://swagger.io/docs/specification/openapi-extensions/) to schemas to indicate source java class.

* `addXJavaComparable`<br>
type: `boolean`<br>
default: `false`

Include additional boolean `x-java-comparable` [extension](https://swagger.io/docs/specification/openapi-extensions/) to schemas to indicate if source java class implements `java.lang.Comparable` interface. Note, that `java.net.URL` does not implement it.

* `addXLinkedEntity`<br>
type: `boolean`<br>
default: `false`

Include additional `x-linked-entity` [extension](https://swagger.io/docs/specification/openapi-extensions/) to every `link` entry to indicate type of entity returned by corresponding `href` URL.

* `info`<br>
type: [`Info`](https://github.com/swagger-api/swagger-core/blob/master/modules/swagger-models/src/main/java/io/swagger/v3/oas/models/info/Info.java)

Bean to be included as `info` part to OpenAPI spec. Plugin will try to fill some fields by default since some of them are required.

* `servers`<br>
type: `List<`[`Server`](https://github.com/swagger-api/swagger-core/blob/master/modules/swagger-models/src/main/java/io/swagger/v3/oas/models/servers/Server.java)`>`<br>
default: single server instance with `/api` url

Beans to be included as `servers` part to OpenAPI spec.

### Naming properies

* `baseTypePrefix`<br>
type: `String`<br>
default: `"Base"`

Prefix to add to component's name that contains properties from root classes when inheritance is used.

* `defaultTypeSuffix`<br>
type: `String`<br>
default: `""`

Suffix to add to TypeScript interfaces to everything except enum classes and other special cases

* `enumTypeSuffix`<br>
type: `String`<br>
default: `""`

Suffix to add to TypeScript interfaces generated from enums

* `linksTypeSuffix`<br>
type: `String`<br>
default: `"Links"`

Suffix to add to class names that contains the single `_links` field for each exposed class. Must be different from `defaultTypeSuffix`.

* `linkTypeName`<br>
type: `String`<br>
default: `"Link"`

How to name TypeScript interface representing `org.springframework.hateoas.Link` data class.

* `withLinksTypeSuffix`<br>
type: `String`<br>
default: `"WithLinks"`

Suffix to add to interface names that contains both `_links` field and all other properties for exposed class. Such types are usual result of findOne() operation.

* `withProjectionsTypeSuffix`<br>
type: `String`<br>
default: `"WithLinks"`

Prefix to add to the name of composed schema with all projection of some entity.

* `withProjectionsTypeSuffix`<br>
type: `String`<br>
default: `"WithLinks"`

Suffix to add to the name of composed schema with all projection of some entity.
