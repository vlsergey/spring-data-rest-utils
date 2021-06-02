# Gradle plugin for Spring Data JPA REST and OpenAPI  

Generates [OpenAPI specification](https://swagger.io/specification/) from JPA repositories exposed via [Spring Data REST](https://spring.io/projects/spring-data-rest) library.

- [x] Generates schema components for both exposed and non-exposed classes
- [ ] (WIP) Generates path and operations for supported commands

## Enabling Gradle plugin
Using the plugins DSL:
```groovy
plugins {
  id "io.github.vlsergey.spring-data-rest-utils" version "0.6.1"
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
    classpath "io.github.vlsergey.springdatarestutils:spring-data-rest-utils:0.6.1"
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
| info                        | [`Info`](https://github.com/swagger-api/swagger-core/blob/master/modules/swagger-models/src/main/java/io/swagger/v3/oas/models/info/Info.java) | `""`        | Bean to be included as `info` part to OpenAPI spec. Plugin will try to fill some fields by default since some of them are required.
| linkTypeName                | `String`  | `"LinkType"`   | How to name TypeScript interface representing `org.springframework.hateoas.Link` data class.
| output                      | `File`    | `"api.yaml"`   | Where to output result. Supports both `.json` and `.yaml` extensions.
| servers                     | `List<`[`Server`](https://github.com/swagger-api/swagger-core/blob/master/modules/swagger-models/src/main/java/io/swagger/v3/oas/models/servers/Server.java)`>` | single server instance with `/api` url | Beans to be included as `servers` part to OpenAPI spec.
| repositoryDetectionStrategy | `RepositoryDetectionStrategies` | `DEFAULT` | The strategy to determine whether a given repository is to be exported by Spring Data REST. Values (and actual implementation) are reused from [Spring Data REST `RepositoryDetectionStrategies`](https://docs.spring.io/spring-data/rest/docs/current/api/org/springframework/data/rest/core/mapping/RepositoryDetectionStrategy.RepositoryDetectionStrategies.html)
| typeSuffix          | `String`  | `""`           | Suffix to add to all TypeScript interfaces _without `_links`_ field. I.e. to all non-exposed (included) entities, enums, etc.
| withLinksTypeSuffix | `String`  | `"WithLinks"`  | Suffix to add to top-level exposed entities, i.e. TypeScript interfaces with `_links` field. Must be different from `typeSuffix`.
