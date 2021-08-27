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

	getAddXCustomAnnotations().convention(defaults.getAddXCustomAnnotations());
	getAddXJavaComparable().convention(defaults.isAddXJavaComparable());
	getAddXJavaClassName().convention(defaults.isAddXJavaClassName());
	getAddXLinkedEntity().convention(defaults.isAddXLinkedEntity());
	getBasePackage().convention(defaults.getBasePackage());
	getBaseTypePrefix().convention(defaults.getBaseTypePrefix());
	getCreateTypePrefix().convention(defaults.getCreateTypePrefix());
	getCreateTypeSuffix().convention(defaults.getCreateTypeSuffix());
	getDefaultTypeSuffix().convention(defaults.getDefaultTypeSuffix());
	getEnumTypeSuffix().convention(defaults.getEnumTypeSuffix());
	getInfo().convention(defaults.getInfo());
	getLinkTypeName().convention(defaults.getLinkTypeName());
	getLinksTypeSuffix().convention(defaults.getLinksTypeSuffix());
	getPatchTypeSuffix().convention(defaults.getPatchTypeSuffix());
	getRepositoryDetectionStrategy().convention(defaults.getRepositoryDetectionStrategy());
	getOutput().convention(() -> {
	    try {
		return new File(new URI(defaults.getOutputUri()));
	    } catch (URISyntaxException e) {
		throw new RuntimeException(e);
	    }
	});
	getServers().convention(defaults.getServers());
	getUpdateTypePrefix().convention(defaults.getUpdateTypePrefix());
	getUpdateTypeSuffix().convention(defaults.getUpdateTypeSuffix());
	getWithLinksTypeSuffix().convention(defaults.getWithLinksTypeSuffix());
    }

    abstract ListProperty<String> getAddXCustomAnnotations();

    abstract Property<Boolean> getAddXJavaClassName();

    abstract Property<Boolean> getAddXJavaComparable();

    abstract Property<Boolean> getAddXLinkedEntity();

    abstract Property<String> getBasePackage();

    abstract Property<String> getBaseTypePrefix();

    abstract Property<String> getCreateTypePrefix();

    abstract Property<String> getCreateTypeSuffix();

    abstract Property<String> getDefaultTypeSuffix();

    abstract Property<String> getEnumTypeSuffix();

    abstract Property<Info> getInfo();

    abstract Property<Integer> getLinkDepth();

    abstract Property<String> getLinksTypeSuffix();

    abstract Property<String> getLinkTypeName();

    abstract RegularFileProperty getOutput();

    abstract Property<String> getPatchTypeSuffix();

    abstract Property<String> getRepositoryDetectionStrategy();

    abstract ListProperty<Server> getServers();

    abstract Property<String> getUpdateTypePrefix();

    abstract Property<String> getUpdateTypeSuffix();

    abstract Property<String> getWithLinksTypeSuffix();

    TaskProperties toTaskProperties() {
	return new TaskProperties() //
		.setAddXCustomAnnotations(getAddXCustomAnnotations().get()) //
		.setAddXJavaClassName(getAddXJavaClassName().get()) //
		.setAddXJavaComparable(getAddXJavaComparable().get()) //
		.setAddXLinkedEntity(getAddXLinkedEntity().get()) //
		.setBasePackage(getBasePackage().get()) //
		.setBaseTypePrefix(getBaseTypePrefix().get()) //
		.setCreateTypePrefix(getCreateTypePrefix().get()) //
		.setCreateTypeSuffix(getCreateTypeSuffix().get()) //
		.setDefaultTypeSuffix(getDefaultTypeSuffix().get()) //
		.setEnumTypeSuffix(getEnumTypeSuffix().get()) //
		.setInfo(getInfo().get()) //
		.setLinksTypeSuffix(getLinksTypeSuffix().get()) //
		.setLinkTypeName(getLinkTypeName().get()) //
		.setPatchTypeSuffix(getPatchTypeSuffix().get()) //
		.setOutputUri(getOutput().getAsFile().get().toURI().toString()) //
		.setRepositoryDetectionStrategy(getRepositoryDetectionStrategy().get()) //
		.setServers(getServers().get()) //
		.setUpdateTypePrefix(getUpdateTypePrefix().get()) //
		.setUpdateTypeSuffix(getUpdateTypeSuffix().get()) //
		.setWithLinksTypeSuffix(getWithLinksTypeSuffix().get()) //
	;
    }

}
