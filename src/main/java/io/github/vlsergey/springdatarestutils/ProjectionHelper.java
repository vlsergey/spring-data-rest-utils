package io.github.vlsergey.springdatarestutils;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Triple;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import io.github.vlsergey.springdatarestutils.CodebaseScannerFacade.ScanResult;
import lombok.NonNull;

class ProjectionHelper {

    private final @NonNull Map<Class<?>, Map<String, Class<?>>> projections;

    ProjectionHelper(final @NonNull ScanResult scanResult) {
	this.projections = scanResult.getProjections().stream().flatMap(projectionInterface -> {
	    String name = SpringDataUtils.getProjectionName(projectionInterface)
		    .orElse(projectionInterface.getSimpleName());

	    return SpringDataUtils.getProjectionTypes(projectionInterface).map(Arrays::stream).orElse(Stream.empty())
		    .map(type -> Triple.of(type, name, projectionInterface));
	}).collect(groupingBy(Triple::getLeft, toMap(Triple::getMiddle, Triple::getRight)));
    }

    @NonNull
    Optional<Set<String>> getProjectionNames(final @NonNull Class<?> domainType) {
	Map<String, Class<?>> typeProjections = projections.get(domainType);
	return typeProjections == null ? Optional.empty() : Optional.of(typeProjections.keySet());
    }

    @NonNull
    Map<String, Class<?>> getProjections(final @NonNull Class<?> domainType) {
	return projections.getOrDefault(domainType, emptyMap());
    }

    boolean hasProjections(final @NonNull Class<?> domainType) {
	return projections.containsKey(domainType);
    }

}
