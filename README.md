# Gradle plugin for Spring Data JPA REST and OpenAPI  

[![Build with Gradle](https://github.com/vlsergey/spring-data-rest-utils/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/vlsergey/spring-data-rest-utils/actions/workflows/build.yml)
[![Gradle Plugin](https://img.shields.io/maven-metadata/v?label=Gradle%20Plugin&metadataUrl=https://plugins.gradle.org/m2/io/github/vlsergey/spring-data-rest-utils/io.github.vlsergey.spring-data-rest-utils.gradle.plugin/maven-metadata.xml)](https://plugins.gradle.org/plugin/io.github.vlsergey.spring-data-rest-utils)

Generates [OpenAPI specification](https://swagger.io/specification/) from JPA repositories exposed via [Spring Data REST](https://spring.io/projects/spring-data-rest) library.

- [x] Generates schema components for both exposed and non-exposed classes
- [x] Supports simple cases of inheritance with discriminators
- [x] Exposes open projections and default projections
- [X] Generates path and operations for supported commands
  - Generates path items for CRUD methods: `findAll`, `findOneById`, `deleteById`, `save` (incl. PUT, POST and PATCH)
  - Generates path items to fetch linked entities
  - (WIP) Generates path items for query methods (so far that accepts and returns "simple" types like `Long` or `String`)

Examples:
- [Example of generated specification](https://github.com/vlsergey/spring-data-rest-utils/blob/master/src/test/resources/io/github/vlsergey/springdatarestutils/expected-example.yaml). [View in Swagger Editor Online](https://editor.swagger.io/?url=https://raw.githubusercontent.com/vlsergey/spring-data-rest-utils/master/src/test/resources/io/github/vlsergey/springdatarestutils/expected-example.yaml).
- [Example package used for generation](https://github.com/vlsergey/spring-data-rest-utils/tree/master/src/test/java/io/github/vlsergey/springdatarestutils/example)

## Enabling Gradle plugin
Using the plugins DSL:
```groovy
plugins {
  id "io.github.vlsergey.spring-data-rest-utils" version "0.21.0"
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
    classpath "io.github.vlsergey.springdatarestutils:spring-data-rest-utils:0.21.0"
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
    addXLinkedEntity = true;
    addXSortable = true;
    basePackage = 'org.myname.myapp';
    info.get().with {
        title = 'MyApplication Data API';
    }
    servers.set([new io.swagger.v3.oas.models.servers.Server().url('/api/data')]);
    output = file('../frontend/src/MyAppApi.yaml');
    repositoryDetectionStrategy = 'ALL'
}
```

| Property                    | Type      |  Default value | Description |
| --------------------------- | --------- | -------------- | ----------- |
| addXLinkedEntity            | `boolean` | `false`        | Include additional `x-linked-entity` [extension](https://swagger.io/docs/specification/openapi-extensions/) to every `link` entry to indicate type of entity returned by corresponding `href` URL  
| addXSortable                | `boolean` | `false`        | Include additional `x-sortable` [extension](https://swagger.io/docs/specification/openapi-extensions/) to schemas that are sortable from plugin author point of view. Currently it includes all entries implementing `java.lang.Comparable` and `java.net.URL`. Feel free to submit patch to [StandardSchemasProvider.java](https://github.com/vlsergey/spring-data-rest-utils/blob/master/src/main/java/io/github/vlsergey/springdatarestutils/StandardSchemasProvider.java) class better behavior.
| basePackage                 | `String`  | `""`           | Base package to search JPA repository interfaces in.
| baseTypePrefix              | `String`  | `"Base"`       | Prefix to add to component's name that contains properties from root classes when inheritance is used.
| defaultTypeSuffix           | `String`  | `""`           | Suffix to add to TypeScript interfaces to everything except enum classes
| enumTypeSuffix              | `String`  | `""`           | Suffix to add to TypeScript interfaces generated from enums
| info                        | [`Info`](https://github.com/swagger-api/swagger-core/blob/master/modules/swagger-models/src/main/java/io/swagger/v3/oas/models/info/Info.java) | `""`        | Bean to be included as `info` part to OpenAPI spec. Plugin will try to fill some fields by default since some of them are required.
| linksTypeSuffix             | `String`  | `"Links"`      | Suffix to add to class names that contains the single `_links` field for each exposed class. Must be different from `defaultTypeSuffix`.
| linkTypeName                | `String`  | `"Link"`       | How to name TypeScript interface representing `org.springframework.hateoas.Link` data class.
| output                      | `File`    | `"api.yaml"`   | Where to output result. Supports both `.json` and `.yaml` extensions.
| servers                     | `List<`[`Server`](https://github.com/swagger-api/swagger-core/blob/master/modules/swagger-models/src/main/java/io/swagger/v3/oas/models/servers/Server.java)`>` | single server instance with `/api` url | Beans to be included as `servers` part to OpenAPI spec.
| repositoryDetectionStrategy | `String`  | `DEFAULT`      | The strategy to determine whether a given repository is to be exported by Spring Data REST. Values (and actual implementation) are reused from [Spring Data REST `RepositoryDetectionStrategies`](https://docs.spring.io/spring-data/rest/docs/current/api/org/springframework/data/rest/core/mapping/RepositoryDetectionStrategy.RepositoryDetectionStrategies.html)
| withLinksTypeSuffix         | `String`  | `"WithLinks"`  | Suffix to add to interface names that contains both `_links` field and all other properties for exposed class. Such types are usual result of findOne() operation.
