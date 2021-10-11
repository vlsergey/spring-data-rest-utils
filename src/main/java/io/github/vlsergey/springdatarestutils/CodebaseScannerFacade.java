package io.github.vlsergey.springdatarestutils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;
import org.springframework.util.ClassUtils;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
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

	Set<Class<?>> projections = (Set<Class<?>>) SpringDataUtils.CLASS_PROJECTION.map(cls -> {
	    try {
		return reflections.getTypesAnnotatedWith(cls);
	    } catch (ReflectionsException exc) {
		log.info("No types annotated with @Projection were found. Hope you just are not usign them.");
		return emptySet();
	    }
	}).orElse(emptySet());

	final @NonNull Map<Class<?>, SortedSet<Class<?>>> inheritance = scanForInheritance(reflections, repositories);
	final @NonNull Set<Method> queryMethodsCandidates = scanForQueryMethodsCandidates(reflections, repositories);

	return new ScanResult(unmodifiableMap(inheritance), unmodifiableSet(projections), unmodifiableSet(repositories),
		unmodifiableSet(queryMethodsCandidates));
    }

    private @NonNull Map<Class<?>, SortedSet<Class<?>>> scanForInheritance(final Reflections reflections,
	    final Set<RepositoryMetadata> repositories) {
	final Predicate<Class<?>> inBasePackageOrSubpackage = cls -> (cls.getPackage().getName() + ".")
		.startsWith(basePackage + ".");

	Map<Class<?>, SortedSet<Class<?>>> result = new HashMap<>();
	PersistenceUtils.CLASS_INHERITANCE.ifPresent(inheritance -> repositories.stream() //
		.map(RepositoryMetadata::getDomainType) //
		.filter(cls -> cls.isAnnotationPresent(inheritance)) //
		.forEach(parentClass -> {
		    SortedSet<Class<?>> childClasses = new TreeSet<>(Comparator.comparing(Class::getName));

		    childClasses.add(parentClass);
		    reflections.getSubTypesOf(parentClass).stream().filter(inBasePackageOrSubpackage)
			    .forEach(childClasses::add);

		    result.put(parentClass, childClasses);
		}));
	return result;
    }

    private @NonNull Set<Method> scanForQueryMethodsCandidates(final Reflections reflections,
	    final Set<RepositoryMetadata> repositories) {
	log.info("Scanning implementation of repo methods to filter query methods candidates...");
	Set<Method> queryMethodsCandidates = new LinkedHashSet<>();
	for (RepositoryMetadata meta : repositories) {

	    // TODO: not ideal check, better to actually check overriding of methods in base
	    // interface
	    final Set<Method> crudMethods = new HashSet<>();
	    meta.getCrudMethods().getDeleteMethod().ifPresent(crudMethods::add);
	    meta.getCrudMethods().getFindAllMethod().ifPresent(crudMethods::add);
	    meta.getCrudMethods().getFindOneMethod().ifPresent(crudMethods::add);
	    meta.getCrudMethods().getSaveMethod().ifPresent(crudMethods::add);

	    nextMethod: for (Method repoMethod : meta.getRepositoryInterface().getMethods()) {
		Method method = ClassUtils.getMostSpecificMethod(repoMethod, meta.getRepositoryInterface());
		if (method.isBridge() || method.isDefault() || Modifier.isStatic(method.getModifiers())
			|| method.getDeclaringClass().getName().startsWith("org.springframework.")
			|| crudMethods.contains(method)) {
		    continue;
		}

		for (Class<?> subType : reflections.getSubTypesOf(method.getDeclaringClass())) {
		    final @NonNull Method declared;
		    try {
			declared = subType.getDeclaredMethod(method.getName(), method.getParameterTypes());
		    } catch (NoSuchMethodException exc) {
			continue;
		    }

		    if (subType.isInterface()) {
			if (declared.isDefault()) {
			    log.debug(
				    "Method {} is not a query candidate because have non-default declaration in {}, "
					    + "which is child class of {}",
				    method, subType.getName(), method.getDeclaringClass().getName());
			    continue nextMethod;
			}
		    } else {
			if (!Modifier.isAbstract(declared.getModifiers())) {
			    log.debug(
				    "Method {} is not a query candidate because have non-abstract declaration in {}, "
					    + "which is child class of {}",
				    method, subType.getName(), method.getDeclaringClass().getName());
			    continue nextMethod;
			}
		    }
		}
		log.info("Method {} is registered as a query candidate", method);
		queryMethodsCandidates.add(method);
	    }
	}
	return queryMethodsCandidates;
    }

    @AllArgsConstructor
    @Data
    public static class ScanResult {
	/**
	 * key class has @Inheritance annotation, child class is not an interface and
	 * present in base (sub)package
	 */
	private final @NonNull Map<Class<?>, SortedSet<Class<?>>> inheritance;
	private final @NonNull Set<Class<?>> projections;
	private final @NonNull Set<RepositoryMetadata> repositories;
	private final @NonNull Set<Method> queryMethodsCandidates;
    }

}
