package io.github.vlsergey.springdatarestutils;

import java.io.File;
import java.util.ArrayList;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

abstract class PluginProperties {

    public PluginProperties() {
	getAddXLinkedEntity().convention(Boolean.FALSE);
	getAddXSortable().convention(Boolean.FALSE);
	getBasePackage().convention((String) null);
	getEnumSuffix().convention("");
	getInfo().convention(new Info());
	getLinkTypeName().convention("LinkType");
	getRepositoryDetectionStrategy().convention("DEFAULT");
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

    abstract ListProperty<Server> getServers();

    abstract Property<String> getTypeSuffix();

    abstract Property<String> getWithLinksTypeSuffix();

    TaskProperties toTaskProperties() {
	return new TaskProperties() //
		.setAddXLinkedEntity(getAddXLinkedEntity().get()) //
		.setAddXSortable(getAddXSortable().get()).setBasePackage(getBasePackage().get()) //
		.setEnumSuffix(getEnumSuffix().get()).setInfo(getInfo().get()) //
		.setLinkTypeName(getLinkTypeName().get()) //
		.setOutputUri(getOutput().getAsFile().get().toURI().toString()) //
		.setRepositoryDetectionStrategy(getRepositoryDetectionStrategy().get()) //
		.setServers(getServers().get()).setTypeSuffix(getTypeSuffix().get()) //
		.setWithLinksTypeSuffix(getWithLinksTypeSuffix().get());
    }

}
