package io.github.vlsergey.springdatarestutils;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

abstract class PluginProperties {

    public PluginProperties() {
	final TaskProperties defaults = new TaskProperties();

	getAddXLinkedEntity().convention(defaults.isAddXLinkedEntity());
	getAddXSortable().convention(defaults.isAddXSortable());
	getBasePackage().convention(defaults.getBasePackage());
	getBaseTypePrefix().convention(defaults.getBaseTypePrefix());
	getDefaultTypeSuffix().convention(defaults.getDefaultTypeSuffix());
	getEnumTypeSuffix().convention(defaults.getEnumTypeSuffix());
	getInfo().convention(defaults.getInfo());
	getLinkTypeName().convention(defaults.getLinkTypeName());
	getLinksTypeSuffix().convention(defaults.getLinksTypeSuffix());
	getPatchTypeSuffix().convention(defaults.getPatchTypeSuffix());
	getRepositoryDetectionStrategy().convention(defaults.getRepositoryDetectionStrategy());
	getRequestTypeSuffix().convention(defaults.getRequestTypeSuffix());
	getOutput().convention(() -> {
	    try {
		return new File(new URI(defaults.getOutputUri()));
	    } catch (URISyntaxException e) {
		throw new RuntimeException(e);
	    }
	});
	getServers().convention(defaults.getServers());
	getWithLinksTypeSuffix().convention(defaults.getWithLinksTypeSuffix());
    }

    abstract Property<Boolean> getAddXLinkedEntity();

    abstract Property<Boolean> getAddXSortable();

    abstract Property<String> getBasePackage();

    abstract Property<String> getBaseTypePrefix();

    abstract Property<String> getDefaultTypeSuffix();

    abstract Property<String> getEnumTypeSuffix();

    abstract Property<Info> getInfo();

    abstract Property<Integer> getLinkDepth();

    abstract Property<String> getLinksTypeSuffix();

    abstract Property<String> getLinkTypeName();

    abstract RegularFileProperty getOutput();

    abstract Property<String> getPatchTypeSuffix();

    abstract Property<String> getRepositoryDetectionStrategy();

    abstract Property<String> getRequestTypeSuffix();

    abstract ListProperty<Server> getServers();

    abstract Property<String> getWithLinksTypeSuffix();

    TaskProperties toTaskProperties() {
	return new TaskProperties() //
		.setAddXLinkedEntity(getAddXLinkedEntity().get()) //
		.setAddXSortable(getAddXSortable().get()) //
		.setBasePackage(getBasePackage().get()) //
		.setBaseTypePrefix(getBaseTypePrefix().get()) //
		.setDefaultTypeSuffix(getDefaultTypeSuffix().get()) //
		.setEnumTypeSuffix(getEnumTypeSuffix().get()) //
		.setInfo(getInfo().get()) //
		.setLinksTypeSuffix(getLinksTypeSuffix().get()) //
		.setLinkTypeName(getLinkTypeName().get()) //
		.setPatchTypeSuffix(getPatchTypeSuffix().get()) //
		.setOutputUri(getOutput().getAsFile().get().toURI().toString()) //
		.setRepositoryDetectionStrategy(getRepositoryDetectionStrategy().get()) //
		.setRequestTypeSuffix(getRequestTypeSuffix().get()) //
		.setServers(getServers().get()) //
		.setWithLinksTypeSuffix(getWithLinksTypeSuffix().get()) //
	;
    }

}
