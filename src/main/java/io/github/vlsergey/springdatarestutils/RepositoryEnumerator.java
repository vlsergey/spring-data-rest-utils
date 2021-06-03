package io.github.vlsergey.springdatarestutils;

import java.util.Arrays;
import java.util.Set;

import javax.annotation.Nullable;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.SubTypesScanner;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;

import static java.util.stream.Collectors.toSet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class RepositoryEnumerator {

    private static final String ANN_NO_REPOSITORY_BEAN = "org.springframework.data.repository.NoRepositoryBean";

    @Getter
    private final @Nullable String basePackage;

    @Getter
    private final @NonNull RepositoryDetectionStrategy repositoryDetectionStrategy;

    @SneakyThrows
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Set<RepositoryMetadata> enumerate(ClassLoader classLoader) {
	Class<?> jpaRepositoryInterface = Class.forName("org.springframework.data.jpa.repository.JpaRepository", false,
		classLoader);

	final Reflections reflections = basePackage == null ? new Reflections(classLoader, new SubTypesScanner(true))
		: new Reflections(basePackage, classLoader, new SubTypesScanner(true));

	final Set<Class<?>> jpaRepos;
	try {
	    jpaRepos = (Set) reflections.getSubTypesOf(jpaRepositoryInterface);
	} catch (ReflectionsException exc) {
	    throw new RuntimeException(
		    "Unable to locate any JPA repositories in package or subpackages of '" + basePackage + "'");
	}

	return jpaRepos.stream() //
		.filter(Class::isInterface) //
		.filter(cls -> basePackage == null || cls.getName().startsWith(basePackage)) //
		.filter(cls -> Arrays.stream(cls.getAnnotations())
			.noneMatch(ann -> ann.annotationType().getName().contentEquals(ANN_NO_REPOSITORY_BEAN)))
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
    }

}
