package io.github.vlsergey.springdatarestutils;

import java.io.File;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;

import io.swagger.v3.oas.models.info.Info;

abstract class PluginProperties {

    public PluginProperties() {
	getBasePackage().convention((String) null);
	getEnumSuffix().convention("");
	getInfo().convention(new Info());
	getLinkTypeName().convention("LinkType");
	getRepositoryDetectionStrategy().convention(RepositoryDetectionStrategies.DEFAULT);
	getOutput().convention(() -> new File("api.yaml"));
	getTypeSuffix().convention("");
	getWithLinksTypeSuffix().convention("WithLinks");
    }

    abstract Property<String> getBasePackage();

    abstract Property<String> getEnumSuffix();

    abstract Property<Info> getInfo();

    abstract Property<String> getLinkTypeName();

    abstract RegularFileProperty getOutput();

    abstract Property<RepositoryDetectionStrategies> getRepositoryDetectionStrategy();

    abstract Property<String> getTypeSuffix();

    abstract Property<String> getWithLinksTypeSuffix();

}
