package io.github.vlsergey.springdatarestutils.example;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface StudentRepository extends JpaRepository<Student, UUID>, QuerydslPredicateExecutor<Student> {

    Long countByGroupGroupId(UUID groupId);

}
