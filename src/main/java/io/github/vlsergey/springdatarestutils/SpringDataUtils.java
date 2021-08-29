package io.github.vlsergey.springdatarestutils;

import java.util.Optional;

class SpringDataUtils {

    static final Optional<Class<?>> CLASS_PAGEABLE = ReflectionUtils
	    .findClass("org.springframework.data.domain.Pageable");

    static final Optional<Class<?>> CLASS_QIERYDSL_PREDICATE = ReflectionUtils
	    .findClass("com.querydsl.core.types.Predicate");

    static final Optional<Class<?>> CLASS_QIERYDSL_PREDICATE_EXECUTOR = ReflectionUtils
	    .findClass("org.springframework.data.querydsl.QuerydslPredicateExecutor");

    static final Optional<Class<?>> CLASS_SIMPLE_JPA_REPOSITORY = ReflectionUtils
	    .findClass("org.springframework.data.jpa.repository.support.SimpleJpaRepository");

}
