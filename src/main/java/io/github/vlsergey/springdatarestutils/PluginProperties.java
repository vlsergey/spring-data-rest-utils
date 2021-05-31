package io.github.vlsergey.springdatarestutils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import lombok.AccessLevel;
import lombok.Getter;

abstract class PluginProperties {

    @Internal
    @Getter(value = AccessLevel.PACKAGE)
    final Provider<RepositoryDetectionStrategies> repositoryDetectionStrategyEnum = getRepositoryDetectionStrategy()
	    .map(in -> {
		try {
		    return RepositoryDetectionStrategies.valueOf(in.trim().toUpperCase());
		} catch (IllegalArgumentException exc) {
		    return RepositoryDetectionStrategies.DEFAULT;
		}
	    });

    public PluginProperties() {
	getAddXLinkedEntity().convention(Boolean.FALSE);
	getAddXSortable().convention(Boolean.FALSE);
	getBasePackage().convention((String) null);
	getEnumSuffix().convention("");
	getInfo().convention(new Info());
	getLinkTypeName().convention("LinkType");
	getRepositoryDetectionStrategy().convention(RepositoryDetectionStrategies.DEFAULT.name());
	getOutput().convention(() -> new File("api.yaml"));

	final ArrayList<Server> defaultServers = new ArrayList<>();
	defaultServers.add(new Server().url("/api"));
	getServers().convention(defaultServers);

	getTypeSuffix().convention("");
	getWithLinksTypeSuffix().convention("WithLinks");
    }

    abstract Property<Boolean> getAddXLinkedEntity();

    abstract Property<Boolean> getAddXSortable();

    abstract Property<String> getBasePackage();

    abstract Property<String> getEnumSuffix();

    abstract Property<Info> getInfo();

    abstract Property<String> getLinkTypeName();

    abstract RegularFileProperty getOutput();

    abstract Property<String> getRepositoryDetectionStrategy();

    abstract Property<List<Server>> getServers();

    abstract Property<String> getTypeSuffix();

    abstract Property<String> getWithLinksTypeSuffix();

}
