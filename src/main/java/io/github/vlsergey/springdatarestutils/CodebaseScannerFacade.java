package io.github.vlsergey.springdatarestutils;

import java.util.Set;

import javax.annotation.Nullable;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class CodebaseScannerFacade {

    @Getter
    private final @Nullable String basePackage;

    @Getter
    private final @NonNull RepositoryDetectionStrategy repositoryDetectionStrategy;

    @SneakyThrows
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ScanResult scan(ClassLoader classLoader) {
	final Reflections reflections = basePackage == null
		? new Reflections(classLoader, new SubTypesScanner(true), new TypeAnnotationsScanner())
		: new Reflections(basePackage, classLoader, new SubTypesScanner(true), new TypeAnnotationsScanner());

	final Set<Class<?>> jpaRepos;
	try {
	    jpaRepos = (Set) reflections.getSubTypesOf(JpaRepository.class);
	} catch (ReflectionsException exc) {
	    throw new RuntimeException(
		    "Unable to locate any JPA repositories in package or subpackages of '" + basePackage + "'");
	}

	final Set<RepositoryMetadata> repositories = jpaRepos.stream() //
		.filter(Class::isInterface) //
		.filter(cls -> basePackage == null || cls.getName().startsWith(basePackage)) //
		.filter(cls -> cls.getAnnotation(NoRepositoryBean.class) == null) //
		.map(cls -> {
		    log.debug("Found JPA repo class: {}", cls.getName());
		    return cls;
		}) //
		.map(AbstractRepositoryMetadata::getMetadata) //
		.filter(metadata -> {
		    if (!repositoryDetectionStrategy.isExported(metadata)) {
			log.debug("JPA repo class '{}' is ignored per detection strategy",
				metadata.getRepositoryInterface().getName());
			return false;
		    }
		    log.info("Collected JPA repo class: {}", metadata.getRepositoryInterface().getName());
		    return true;
		}).collect(toSet());

	Set<Class<?>> projections = emptySet();
	try {
	    projections = reflections.getTypesAnnotatedWith(Projection.class);
	} catch (ReflectionsException exc) {
	    log.info("No types annotated with @Projection were found. Hope you just are not usign them.");
	}

	return new ScanResult(projections, repositories);
    }

    @AllArgsConstructor
    @Data
    public static class ScanResult {
	private final @NonNull Set<Class<?>> projections;
	private final @NonNull Set<RepositoryMetadata> repositories;
    }

}
